package com.niko.Service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.niko.Service.DealNumberService;
import com.niko.dto.ExcelWriteDto;
import com.niko.dto.Recharge;
import com.niko.Service.RechargeService;
import com.niko.dto.RechargeAmountExcel;
import com.niko.mapper.RechargeMapper;
import com.niko.util.MyUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.ibatis.executor.BatchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【recharge(充值表)】的数据库操作Service实现
* @createDate 2024-09-02 16:53:18
*/
@Service
public class RechargeServiceImpl extends ServiceImpl<RechargeMapper, Recharge>
    implements RechargeService{

    @Autowired
    private DealNumberService   dealNumberService;
    @Autowired
    private RechargeMapper rechargeMapper;


    @Override
    public String getRechargeExcel(String text, BigDecimal rechargeAmount) {

        if (ObjectUtil.isEmpty(rechargeAmount)) {
            rechargeAmount = new BigDecimal(250);
        }
        List<Recharge> firstToData = new ArrayList<>();
        List<Recharge> haveData = new ArrayList<>();
        //获取账户信息
        List<Recharge> toRecharge = this.getToRecharge(text);

        List<RechargeAmountExcel> rechargeAmountExcels = new ArrayList<>();
        for (Recharge recharge : toRecharge) {
            Recharge rechargeFromData = this.getById(recharge.getId());
            //当前充值金额
            BigDecimal rechargeNow = BigDecimal.ZERO;
            //新的累计金额
            BigDecimal rechargeCountNow = BigDecimal.ZERO;
            if (ObjectUtil.isNotEmpty(rechargeFromData)) {
                //数据库存在,不是第一次
                //获取数据库中累计充值金额
                BigDecimal rechargeCountOld = rechargeFromData.getRechargeCount();
                rechargeNow = getRechargeAmount(recharge, rechargeCountOld, rechargeAmount);
                //计算新的累计金额
                rechargeCountNow = rechargeCountOld.add(rechargeNow);
                recharge.setRechargeCount(rechargeCountNow);
                haveData.add(recharge);
            } else {
                //数据库不存在 ， 第一次
                rechargeNow = getRechargeAmount(recharge,null,rechargeAmount);
                //计算新的累计金额
                rechargeCountNow = rechargeNow;
                recharge.setRechargeCount(rechargeCountNow);
                firstToData.add(recharge);
            }
            RechargeAmountExcel rechargeAmountExcel = new RechargeAmountExcel();
            rechargeAmountExcel.setAccountId(recharge.getAccountId());
            rechargeAmountExcel.setRechargeAmount(rechargeNow.toString());
            rechargeAmountExcels.add(rechargeAmountExcel);
        }
        List<BatchResult> results = new ArrayList<>();
        if (!CollectionUtils.isEmpty(firstToData)) {
            results = rechargeMapper.insert(toRecharge);
        }
        if (!CollectionUtils.isEmpty(haveData)) {
            results = rechargeMapper.updateById(toRecharge);
        }
        results.forEach(System.out::println);
        String fileName = "D:\\carl\\充值.xlsx";
        EasyExcel.write(fileName, RechargeAmountExcel.class).sheet("数据").doWrite(rechargeAmountExcels);

//        int insert = rechargeMapper.insert(toRecharge.get(0));
        return "成功：D:\\carl\\充值.xlsx";
    }

    /**
     * 计算还需充值多少
     * @param recharge
     * @param rechargeCountOld
     * @param rechargeAmount
     * @return
     */
    private BigDecimal getRechargeAmount(Recharge recharge, BigDecimal rechargeCountOld, BigDecimal rechargeAmount) {
        BigDecimal rechargeNow = BigDecimal.ZERO;
        if (ObjectUtil.isNotEmpty(rechargeCountOld)) {
            //累计充值金额减去花费的，获得余额
            BigDecimal subtract = rechargeCountOld.subtract(recharge.getSpend());
            //总共的充值金额减去余额，获得需要充值的金额
            rechargeNow = rechargeAmount.subtract(subtract);
            //去除小数位
        } else {
            //第一次
//            if (recharge.getSpend().compareTo(BigDecimal.ZERO) > 0) {
//
//            }
            rechargeNow = rechargeAmount.subtract(recharge.getSpend());
        }

        return new BigDecimal(rechargeNow.intValue());
    }

    /**
     *  处理数据
     * @param text
     * @return
     */
    public List<Recharge> getToRecharge(String text) {
        List<String> list1 = Arrays.stream(text.split("\\$")).collect(Collectors.toList());
        List<Recharge> recharges = new ArrayList<>();
        String s1 = null;  //保存产品
        int i = 0;
        for (int i1 = 0; i1 < list1.size(); i1++) {
            String s = list1.get(i1);
            if (!s.contains("无支付方式")) {
                List<String> collect = Arrays.stream(s.split(" ")).collect(Collectors.toList());
                Recharge recharge = new Recharge();

                recharge.setTime(new Date());
                //花费金额
                if (i+1 != list1.size()) {
                    String spend = list1.get(i + 1).split(" ")[0];
                    recharge.setSpend(new BigDecimal(spend).setScale(2, BigDecimal.ROUND_HALF_UP));
                }
                //账号存活状态
                if (s.contains("使用中")) {
                    recharge.setActive(0);
                } else {
                    recharge.setActive(1);
                }
                for (String e : collect) {
                    if(e.contains("-") && !e.contains("：")) {
                        recharge.setAccountName(e); //账户名称
//                        List<String> collect1 = Arrays.stream(e.split("-")).collect(Collectors.toList());
                    }
                    if (e.startsWith("编号")) {
                        String accountId = e.substring(e.indexOf("：") + 1);
                        recharge.setAccountId(accountId);//账户编号
                    }

                }
                if (ObjectUtils.isNotEmpty(recharge.getAccountId())) {
                    recharges.add(recharge);
                    //id唯一
                    LambdaQueryWrapper<Recharge> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(Recharge::getAccountId, recharge.getAccountId());
                    Recharge one = this.getOne(wrapper);
                    if (ObjectUtil.isNotEmpty(one)) {
                        recharge.setId(one.getId());
                    }else {
                        recharge.setId(MyUtil.getUUID());
                    }

                }
            }
        }
        return recharges;
    }
}




