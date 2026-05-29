package com.niko.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 充值表
 * @TableName recharge
 */
@TableName(value ="recharge")
@Data
public class Recharge implements Serializable {
    /**
     * 主键id
     */
    @TableId
    private String id;

    /**
     * 账户id
     */
    private String accountId;

    /**
     * 账户名称
     */
    private String accountName;

    /**
     * 充值金额 
     */
    private BigDecimal rechargeCount;

    /**
     * 花费金额
     */
    private BigDecimal spend;

    /**
     * 状态，0使用中，1停用
     */
    private Integer active;

    /**
     * 时间
     */
    private Date time;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        Recharge other = (Recharge) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getAccountId() == null ? other.getAccountId() == null : this.getAccountId().equals(other.getAccountId()))
            && (this.getAccountName() == null ? other.getAccountName() == null : this.getAccountName().equals(other.getAccountName()))
            && (this.getRechargeCount() == null ? other.getRechargeCount() == null : this.getRechargeCount().equals(other.getRechargeCount()))
            && (this.getActive() == null ? other.getActive() == null : this.getActive().equals(other.getActive()))
            && (this.getTime() == null ? other.getTime() == null : this.getTime().equals(other.getTime()));
    }



    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", accountId=").append(accountId);
        sb.append(", accountName=").append(accountName);
        sb.append(", recharge=").append(rechargeCount);
        sb.append(", active=").append(active);
        sb.append(", time=").append(time);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}