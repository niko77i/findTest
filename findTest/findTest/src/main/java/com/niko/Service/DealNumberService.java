package com.niko.Service;

import com.niko.dto.Recharge;

import java.util.List;

public interface DealNumberService {
    String getNumber(String text);

    //获取存活的number
    String getActiveNumber(String text);
    String getActiveNumber1(String text, Integer parm);
     List<Recharge> getToRecharge(String text);

    String getAccountAndAmount(String text);
}
