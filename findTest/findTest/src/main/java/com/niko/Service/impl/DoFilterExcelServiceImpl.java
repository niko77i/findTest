package com.niko.Service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.niko.Service.DoFilterExcelService;
import com.niko.dto.ExcelWriteDto;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoFilterExcelServiceImpl implements DoFilterExcelService {

    @Override
    public void filterExcel(InputStream FileinputStream, String writeFileName) {
        try {
            //读取数据
            List<Map<String, Object>> list = readExcelToMap(FileinputStream, null);
            List<ExcelWriteDto> lists = new ArrayList<>();
            List<String> sheetNames = new ArrayList<>();
            List<String> co  = new ArrayList<>();


            if (list.get(0).keySet().contains("广告系列名称")) {
                list.stream().forEach(map -> {
                    String val = map.get("广告系列名称").toString();
                    String sheetName = map.get("广告系列名称").toString();
                    if (val.contains(" ")) {
                        sheetName = val.substring(0, val.indexOf(" "));
                    }
                    sheetNames.add(sheetName);
                });
                co = sheetNames.stream().distinct().collect(Collectors.toList());
            }
//            System.out.println(sheetNames);

            //过滤数据
            for (Map<String, Object> map : list) {
                ExcelWriteDto excelWriteDto = new ExcelWriteDto();
                if (map.get("花费金额 (USD)") != null && map.get("花费金额 (USD)") != "") {
                    if (ObjectUtils.isNotEmpty(map.get("帐户名称"))) {
                        excelWriteDto.setAccountName(map.get("帐户名称").toString());
                    }
                    if (ObjectUtils.isNotEmpty(map.get("帐户编号"))) {
                        excelWriteDto.setAccountId(map.get("帐户编号").toString());
                    }
                    if (ObjectUtils.isNotEmpty(map.get("花费金额 (USD)"))) {
                        excelWriteDto.setAmount(map.get("花费金额 (USD)").toString());
                    }
                    if (ObjectUtils.isNotEmpty(map.get("广告系列名称"))) {
                        if (map.get("广告系列名称").toString().contains(" ")) {
                            excelWriteDto.setAdName(map.get("广告系列名称").toString().substring(0, map.get("广告系列名称").toString().indexOf(" ")));
                        } else {
                            excelWriteDto.setAdName(map.get("广告系列名称").toString());
                        }
                    }
                    if (ObjectUtils.isNotEmpty(excelWriteDto)) {
                        lists.add(excelWriteDto);
                    }
                }
            }



            if (StringUtils.isAllBlank(lists.get(0).getAdName())) {
                EasyExcel.write(writeFileName,ExcelWriteDto.class).sheet().doWrite(lists);
            } else {
                Map<String, List<ExcelWriteDto>> collect = lists.stream().collect(Collectors.groupingBy(ExcelWriteDto::getAdName));
                Map<String, List<ExcelWriteDto>> result = new HashMap<>();
                for (Map.Entry<String, List<ExcelWriteDto>> entry : collect.entrySet()) {
                    String key = entry.getKey();
                    List<ExcelWriteDto> value = entry.getValue();

                    Map<String, List<ExcelWriteDto>> collect1 = value.stream().collect(Collectors.groupingBy(ExcelWriteDto::getAccountId));
                    List<ExcelWriteDto> result1 = new ArrayList<>();
                    for (Map.Entry<String, List<ExcelWriteDto>> listEntry : collect1.entrySet()) {
                        BigDecimal count = new BigDecimal(0);
                        List<ExcelWriteDto> value1 = listEntry.getValue();
                        ExcelWriteDto excelWriteDto1 = new ExcelWriteDto();
                        if (value1.size() == 1) {
                            count = new BigDecimal(value1.get(0).getAmount());
                            BeanUtils.copyProperties(value1.get(0), excelWriteDto1);
                            excelWriteDto1.setAmount(count.toString());
                        } else {
                            for (ExcelWriteDto excelWriteDto : value1) {
                                count = count.add(new BigDecimal(excelWriteDto.getAmount()));
                            }
                            BeanUtils.copyProperties(value1.get(0), excelWriteDto1);
                            excelWriteDto1.setAmount(count.toString());
                        }
                        result1.add(excelWriteDto1);
                    }
                    result.put(key, result1);
                }

                ExcelWriter excelWriter = EasyExcel.write(writeFileName, ExcelWriteDto.class).build();
                for (int i = 0; i < co.size(); i++) {
                    String key = co.get(i);
                    WriteSheet writeSheet = EasyExcel.writerSheet(i, key).build();
                    excelWriter.write(result.get(key), writeSheet);
                }
                excelWriter.finish();
            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 读取excel,放入List<Map<String, String>>
     *
     * @param FileinputStream 读取excel的流
     * @param sheetName       sheetName
     * @return datalist
     */
    public static List<Map<String, Object>> readExcelToMap(InputStream FileinputStream, String sheetName) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        EasyExcel.read(FileinputStream, new AnalysisEventListener<Map<String, Object>>() {
            //用于存储表头的信息
            private Map<Integer, String> headMap;

            //读取excel表头信息
            @Override
            public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                this.headMap = headMap;
//                System.out.println("表头信息：" + headMap);
            }

            //直接使用Map来保存数据
            @Override
            public void invoke(Map<String, Object> valueData, AnalysisContext context) {
                //把表头和值放入Map
                HashMap<String, Object> paramsMap = new HashMap<>();
                for (int i = 0; i < valueData.size(); i++) {
                    String key = headMap.get(i);
                    Object value = valueData.get(i);
                    if (("帐户编号".equals(key) && (value == null)) || ("帐户名称".equals(key) && (value == null))) {
                        break;
                    }
                    //将表头作为map的key，每行每个单元格的数据作为map的value
                    paramsMap.put(key, value);

                }
                if (!CollectionUtils.isEmpty(paramsMap)) {
                    dataList.add(paramsMap);
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {

            }
        }).sheet(sheetName).doRead();

        return dataList;
    }
}
