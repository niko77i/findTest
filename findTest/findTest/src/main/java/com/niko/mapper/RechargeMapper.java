package com.niko.mapper;

import com.niko.dto.Recharge;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Administrator
* @description 针对表【recharge(充值表)】的数据库操作Mapper
* @createDate 2024-09-02 16:53:18
* @Entity com.niko.dto.Recharge
*/
@Mapper
public interface RechargeMapper extends BaseMapper<Recharge> {

}




