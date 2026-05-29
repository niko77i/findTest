package com.niko.Service;

import com.niko.dto.Recharge;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

/**
* @author Administrator
* @description 针对表【recharge(充值表)】的数据库操作Service
* @createDate 2024-09-02 16:53:18
*/
public interface RechargeService extends IService<Recharge> {
    public String getRechargeExcel(String text, BigDecimal rechargeAmount);
}
