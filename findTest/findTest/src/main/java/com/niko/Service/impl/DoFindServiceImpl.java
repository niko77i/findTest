package com.niko.Service.impl;

import com.alibaba.excel.EasyExcel;
import com.niko.Service.DoFindService;
import com.niko.dto.ExcelWriteDto;
import org.apache.commons.collections4.ListUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DoFindServiceImpl implements DoFindService {

    private final static String CONST_HUI = "回流";

    @Override
    public String find(List<String> urls) {
        List<ExcelWriteDto> lists = new ArrayList<>();
        try {

            if (!CollectionUtils.isEmpty(urls)) {

                //解析url，爬取数据
                explainUrl(urls, lists);

                //存放文件
                writeToLocal(lists);

                return "数据获取成功";
            }

            //存放文件
            writeToLocal(lists);

            return "数据获取成功";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void explainUrl(List<String> urls, List<ExcelWriteDto> lists) throws IOException {
        for (String url : urls) {
            // 发送HTTP GET请求并获取网页内容
            Document document = Jsoup.connect(url).get();
            System.out.print(document);
        }
    }

    private void writeToLocal(List<ExcelWriteDto> list) {
        //设置excel文件路径和文件名称
        String fileName = "D:\\carl\\01.xlsx";

        try {
            if (CollectionUtils.isEmpty(list)) {
                ExcelWriteDto excelWriteDto = new ExcelWriteDto();
                excelWriteDto.setAccountName(CONST_HUI);
                ArrayList<ExcelWriteDto> excelWriteDtos = new ArrayList<>();
                excelWriteDtos.add(excelWriteDto);
                EasyExcel.write(fileName, ExcelWriteDto.class).sheet("数据").doWrite(excelWriteDtos);
            } else {
                EasyExcel.write(fileName, ExcelWriteDto.class).sheet("数据").doWrite(list);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
