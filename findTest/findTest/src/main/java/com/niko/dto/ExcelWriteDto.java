package com.niko.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ExcelWriteDto {
    @ExcelProperty(value = "账户名称", index = 0) //设置表头
    private String accountName;
    @ExcelProperty(value = "账户id", index = 1) //设置表头
    private String accountId;
    @ExcelProperty(value = "空行", index = 2) //设置表头
    private String tttt;
    @ExcelProperty(value = "花费金额", index = 3) //设置表头
    private String amount;
    @ExcelProperty(value = "广告系列", index = 4) //设置表头
    private String adName;
}
