package com.niko.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class RechargeAmountExcel {

    @ExcelProperty(value = "账户id", index = 0) //设置表头
    private String accountId;
    @ExcelProperty(value = "充值金额", index = 1) //设置表头
    private String rechargeAmount;
}
