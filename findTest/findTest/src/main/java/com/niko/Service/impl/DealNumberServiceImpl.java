package com.niko.Service.impl;

import com.google.common.collect.Lists;
import com.niko.Service.DealNumberService;
import com.niko.dto.Recharge;
import com.niko.dto.Text;
import com.niko.util.MyUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.NumberUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class DealNumberServiceImpl implements DealNumberService {

    @Override
    public String getNumber(String text) {
        List<String> numList = new ArrayList<>();
        List<Text> textList = new ArrayList<>();
        List<String> list = Arrays.stream(text.split("\\$")).filter(e -> !e.equals("")).collect(Collectors.toList());
        for (String s : list) {
            Text text1 = new Text();
            List<String> collect = Arrays.stream(s.split(" ")).collect(Collectors.toList());
            for (String e : collect) {
                if (e.contains("-") && !e.contains("：")) {
                    List<String> collect1 = Arrays.stream(e.split("-")).collect(Collectors.toList());
                    String accountName = collect1.get(collect1.size() - 1);
                    text1.setAccountName(Integer.parseInt(accountName));
                }
                if (e.startsWith("编号")) text1.setAccountId(e.substring(e.indexOf("：") + 1));
            }
            if (ObjectUtils.isNotEmpty(text1.getAccountId())) {
                textList.add(text1);
            }

        }

        numList = textList.stream().sorted(Comparator.comparing(Text::getAccountName)).map(Text::getAccountId).collect(Collectors.toList());

        StringBuffer sb = new StringBuffer();
        numList.forEach(e -> sb.append(e + "\n"));
        return sb.toString();
    }

    @Override
    public String getActiveNumber(String text) {
        List<String> list1 = Arrays.stream(text.split("\\$")).collect(Collectors.toList());
        List<String> result = new ArrayList<>();
        List<Text> textList = new ArrayList<>();
        int i = 0;
        String s1 = null;  //保存产品
        for (String s : list1) {
//            List<String> collect = Arrays.stream(s.split(" ")).collect(Collectors.toList());

            Text text1 = new Text();
            List<String> collect = Arrays.stream(s.split(" ")).collect(Collectors.toList());
            if (s.contains("使用中")) {
                for (String e : collect) {
                    if (e.contains("-") && !e.contains("：")) {
                        List<String> collect1 = Arrays.stream(e.split("-")).collect(Collectors.toList());
                        if (StringUtils.isAllBlank(s1)) {
                            s1 = collect1.get(1);
                        }
                        String accountName = collect1.get(collect1.size() - 1);
                        if (ObjectUtils.isEmpty(text1.getAccountName1())) {
                            text1.setAccountName1(accountName);
                        }
                    }
                    if (e.startsWith("编号")) text1.setAccountId(e.substring(e.indexOf("：") + 1));
                }
                if (ObjectUtils.isEmpty(text1.getAccountName1())) {
                    text1.setAccountName1("0");
                }
                if (ObjectUtils.isNotEmpty(text1.getAccountId())) {
                    textList.add(text1);
                }
                /*if (!CollectionUtils.isEmpty(collect) && collect.size() >= 2) {
                    String accountId = collect.get(1).substring(0, collect.get(1).length() - 3);
                    text1.setAccountId(accountId);
                    String e = collect.get(0).substring(0, collect.get(0).length() - 2);
*//*                    if (i == 0) {

                    } else {
                        String s1 = collect.get(0);
                        if (s1.contains(".")) {
                            e = collect.get(0).substring(s1.indexOf(".") + 3, collect.get(0).length() - 2);
                        }
//                    String accountName = collect.get(0).substring(0, collect.get(0).length() - 2);
                    }*//*
                    if (!"".equals(e)) {
                        List<String> collect1 = Arrays.stream(e.split("-")).collect(Collectors.toList());
                        String accountName = collect1.get(collect1.size() - 1);
                        text1.setAccountName(Integer.parseInt(accountName));
                    }
                    i++;
                }

                if (ObjectUtils.isNotEmpty(text1.getAccountId())) {
                    textList.add(text1);
                }*/
            }
        }
        result = textList.stream().sorted(Comparator.comparing(Text::getAccountName1)).map(Text::getAccountId).collect(Collectors.toList());
        StringBuffer sb = new StringBuffer();
        result.forEach(e -> sb.append(e + "\n"));
        return sb.toString();
    }

    @Override
    public String getActiveNumber1(String text, Integer parm) {
        List<String> list1 = Arrays.stream(text.replace("\n"," ").split("\\$")).collect(Collectors.toList());
        List<String> result = new ArrayList<>();
        List<Text> textList = new ArrayList<>();
        String s1 = null;  //保存产品
        int i = 0;
        String regEx = "[\\u4e00-\\u9fa5]+"; //中文正则
        Pattern pat = Pattern.compile(regEx);
        for (String s : list1) {
            List<String> collect = Arrays.stream(s.split(" ")).collect(Collectors.toList());
            if (s.contains("使用中") && !s.contains("无支付方式") ) {
                Text text1 = new Text();
                for (String e : collect) {
                    if (e.contains("-") && !e.contains("：")) {
                        List<String> collect1 = Arrays.stream(e.split("-")).collect(Collectors.toList());
                        if (StringUtils.isAllBlank(s1)) {
                            s1 = collect1.get(1);
                        }
                        String accountName = collect1.get(collect1.size() - 1);
                        if (ObjectUtils.isEmpty(text1.getAccountName())) {
                            text1.setAccountName(Integer.parseInt(accountName));
                        }
                    }
                    if (e.startsWith("编号")) text1.setAccountId(e.substring(e.indexOf("：") + 1));
                }
                if (ObjectUtils.isNotEmpty(text1.getAccountId())) {
                    textList.add(text1);
                }
            }
        }
        result = textList.stream().sorted(Comparator.comparing(Text::getAccountName)).map(Text::getAccountId).collect(Collectors.toList());
        StringBuffer sb = new StringBuffer();
        result.forEach(e -> sb.append(e + "\n"));
        return sb.toString() + "\n" + s1;
    }

    @Override
    public List<Recharge> getToRecharge(String text) {
        List<String> list1 = Arrays.stream(text.split("\\$")).collect(Collectors.toList());
        List<String> result = new ArrayList<>();
//        List<Text> textList = new ArrayList<>();
        List<Recharge> recharges = new ArrayList<>();
        String s1 = null;  //保存产品
        int i = 0;
        for (int i1 = 0; i1 < list1.size(); i1++) {
            String s = list1.get(i1);
            if (!s.contains("无支付方式")) {
                List<String> collect = Arrays.stream(s.split(" ")).collect(Collectors.toList());
                Recharge recharge = new Recharge();

                recharge.setTime(new Date());
//                Text text1 = new Text();
                //花费金额
                if (i + 1 != list1.size()) {
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
                    if (e.contains("-") && !e.contains("：")) {
                        recharge.setAccountName(e); //账户名称
                        List<String> collect1 = Arrays.stream(e.split("-")).collect(Collectors.toList());
                        /*if (StringUtils.isAllBlank(s1)) {
                            s1 = collect1.get(1);
                        }
                        String accountName = collect1.get(collect1.size() - 1);
                        text1.setAccountName(Integer.parseInt(accountName));*/
                    }
                    if (e.startsWith("编号")) {
//                        text1.setAccountId(e.substring(e.indexOf("：")+1));
                        String accountId = e.substring(e.indexOf("：") + 1);
                        recharge.setAccountId(accountId);//账户编号
                    }

                }
                /*if (ObjectUtils.isNotEmpty(text1.getAccountId())) {
                    textList.add(text1);
                }*/
                if (ObjectUtils.isNotEmpty(recharge.getAccountId())) {
                    recharges.add(recharge);

                    recharge.setId(MyUtil.getUUID());
                }
            }
        }
        return recharges;
    }

    @Override
    public String getAccountAndAmount(String text) {
        List<String> split = checkText(text);
        List<String> list = new ArrayList<>();
        StringBuffer stringBuffer = new StringBuffer();
        Pattern pattern = Pattern.compile("[0-9]*");
        //第一个保证为账户编号 或账户名称
        if (split.get(0).contains("-") || (pattern.matcher(split.get(0)).matches() && (split.get(0).length() > 10))) {
            //判断账户名称是否以第一开始，是的话为true
            Boolean flag = false;
            if (split.get(0).contains("-")) {
                flag = true;
            }

            int num = 0;
            for (int i = 0; i < split.size(); i++) {
                if (flag) {
                    //账户名称第一开始
                    if ((split.get(i).contains("-") && i != 0) || (split.get(i).contains("已显示") && i != 0)) {
                        num = i;
                        break;
                    }
                } else {
                    //账户编号第一开始
                    if ((pattern.matcher(split.get(i)).matches() && split.get(i).length() > 10 && i != 0 )|| (split.get(i).contains("已显示") && i != 0)) {
                        num = i;
                        break;
                    }
                }
            }
            List<List<String>> partition = Lists.partition(split, num);
            //判断是否有单次购物费用
            Boolean flagOnce = checkHaveOnceAmount(partition, flag, pattern);
            //获取所需数据
            getTrueText(partition, list, flag, pattern, flagOnce);

            //返回数据
            for (int i = 0; i < list.size(); i++) {
                if (flagOnce) {
                    //返回数据
                    returnData(flag, flagOnce, i, 4, list, stringBuffer);
                } else {
                    returnData(flag, flagOnce, i, 3, list, stringBuffer);
                }

            }
        }

        return stringBuffer.toString();
    }

    private Boolean checkHaveOnceAmount(List<List<String>> split, Boolean flag, Pattern pattern) {
        for (int i = 0; i < split.size()-1; i++) {  //最后一行不用处理
            int accountCountNum = 0;
            int have$CountNum = 0;
            List<String> strings = split.get(i);
            if (strings.contains("$") && strings.contains("-") && strings.contains("—")) {
                //花费了没有成效
                return true;
            } else {
                for (String string : strings) {
                    if (string.contains("-")) accountCountNum++;
                    if (string.contains("$")) have$CountNum++;
                }
                if (accountCountNum < have$CountNum) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 返回数据
     *
     * @param flag
     * @param flagOnce
     * @param i
     * @param num
     * @param list
     * @param stringBuffer
     */
    private void returnData(Boolean flag, Boolean flagOnce, int i, int num, List<String> list, StringBuffer stringBuffer) {
        if ((i + 1) % num == 0) {
            //保证返回值中， 账户名称在第一
            //只需要账户主体
            if (flag) {
                stringBuffer.append(list.get(i + 1 - num).substring(0,list.get(i + 1 - num).indexOf("-"))).append("\t").append(list.get(i + 2 - num)).append("\t\t");
            } else {
                stringBuffer.append(list.get(i + 2 - num).substring(0,list.get(i + 2 - num).indexOf("-"))).append("\t").append(list.get(i + 1 - num)).append("\t\t");
            }
            if (flagOnce) {
                String amountOnce = list.get(i).substring(list.get(i).indexOf("$") + 1, list.get(i).length());
                String amount = list.get(i - 1).substring(list.get(i - 1).indexOf("$") + 1, list.get(i - 1).length());
                amount = amount.replace(",","");
                amountOnce = amountOnce.replace(",","");
                if (StringUtils.isNoneBlank(amount, amountOnce)) {
                    if (new BigDecimal(amount).compareTo(new BigDecimal(amountOnce)) > 0) {
                        stringBuffer.append(amount).append("\n");
                    } else {
                        stringBuffer.append(amountOnce).append("\n");
                    }
                } else {
                    if (StringUtils.isBlank(amount)) {
                        stringBuffer.append(amount).append("\n");
                    } else if (StringUtils.isBlank(amountOnce)) {
                        stringBuffer.append(amountOnce).append("\n");
                    }
                }
            } else {
                String amount = list.get(i).substring(list.get(i).indexOf("$") + 1, list.get(i).length());
                stringBuffer.append(amount).append("\n");
            }
        }
        if (i + 1 == list.size()) {
            stringBuffer.append(list.get(i).substring(list.get(i).indexOf("$") + 1, list.get(i).length()));
        }

    }

    /**
     * 返回数据
     *
     * @param flag
     * @param flagOnce
     * @param i
     * @param num
     * @param list
     * @param stringBuffer
     */
    private void returnDataNew(Boolean flag, Boolean flagOnce, int i, int num, List<String> list, StringBuffer stringBuffer) {
        if ((i + 1) % num == 0) {
            //保证返回值中， 账户名称在第一
            //只需要账户主体
            if (flag) {
                stringBuffer.append(list.get(i + 1 - num).substring(0,list.get(i + 1 - num).indexOf("-"))).append("\t").append(list.get(i + 2 - num)).append("\t");
            } else {
                stringBuffer.append(list.get(i + 2 - num).substring(0,list.get(i + 2 - num).indexOf("-"))).append("\t").append(list.get(i + 1 - num)).append("\t");
            }
            if (flagOnce) {
                String amountOnce = list.get(i).substring(list.get(i).indexOf("$") + 1, list.get(i).length());
                String amount = list.get(i - 1).substring(list.get(i - 1).indexOf("$") + 1, list.get(i - 1).length());
                amount = amount.replace(",","");
                amountOnce = amountOnce.replace(",","");
                if (StringUtils.isNoneBlank(amount, amountOnce)) {
                    if (new BigDecimal(amount).compareTo(new BigDecimal(amountOnce)) > 0) {
                        stringBuffer.append(amount).append("\n");
                    } else {
                        stringBuffer.append(amountOnce).append("\n");
                    }
                } else {
                    if (StringUtils.isBlank(amount)) {
                        stringBuffer.append(amount).append("\n");
                    } else if (StringUtils.isBlank(amountOnce)) {
                        stringBuffer.append(amountOnce).append("\n");
                    }
                }
            } else {
                String amount = list.get(i).substring(list.get(i).indexOf("$") + 1, list.get(i).length());
                stringBuffer.append(amount).append("\n");
            }
        }
        if (i + 1 == list.size()) {
            stringBuffer.append(list.get(i).substring(list.get(i).indexOf("$") + 1, list.get(i).length()));
        }

    }

    /**
     * 获取所需数据
     *
     * @param partition
     * @param list
     * @param flag
     * @param pattern
     */
    private void getTrueText(List<List<String>> partition, List<String> list, Boolean flag, Pattern pattern, Boolean flagOnce) {
        for (int j = 0; j < partition.size(); j++) {
            List<String> split = partition.get(j);
            if (split.get(1).equals("全部")) {

                //选择了组合细分条件
                if (flag) {
                    Boolean flag2 = false; //用于获取不重复的金额
                    //账号名称第一
                    for (int i = 0; i < split.size(); i++) {
                        if (split.get(i).contains("-")) {
                            list.add(split.get(i));
                            flag2 = false;
                        }
                        if (pattern.matcher(split.get(i)).matches() && (split.get(i).length() > 10)) {
                            //账户编号
                            flag2 = true;
                            list.add(split.get(i));
                        }
                        if (flag2) {
                            if (split.get(i).contains("$")) {
                                list.add(split.get(i));
                            }
                        }
                    }
                    if (list.size() % 4 != 0 && flagOnce && list.get(list.size() - 1).contains("$")) {
                        list.add("$0");
                    }

                } else {
                    //账户编号以第一开始
                    Boolean flag2 = false; //用于获取不重复的金额
                    //账号名称第一
                    for (int i = 0; i < split.size(); i++) {
                        if (pattern.matcher(split.get(i)).matches() && (split.get(i).length() > 10)) {
                            list.add(split.get(i));
                            flag2 = false;
                        }
                        if (split.get(i).contains("-")) {
                            //账户名称
                            flag2 = true;
                            list.add(split.get(i));
                        }
                        if (flag2) {
                            if (split.get(i).contains("$")) {
                                list.add(split.get(i));
                            }
                        }
                    }
                    if (list.size() % 4 != 0 && flagOnce && list.get(list.size() - 1).contains("$")) {
                        list.add("$0");
                    }

                }

            } else {
                Boolean flag2 = false;
                String amount1 = "", amount2 = "";
                //取出需要的数据
                for (int i = 0; i < split.size(); i++) {
                    if (split.get(i).contains("-")) {
                        list.add(split.get(i));
                        flag2 = false;
                    }
                    if (pattern.matcher(split.get(i)).matches() && (split.get(i).length() > 10)) {
                        //账户编号
                        flag2 = true;
                        list.add(split.get(i));
                    }
                    if (flag2) {
                        if (split.get(i).contains("$")) {
                            list.add(split.get(i));
                        }
                    }
                    if (j + 1 == partition.size()) {
                        if (split.get(i).contains("$") && StringUtils.isBlank(amount1)) {
                            amount1 = split.get(i).substring(split.get(i).indexOf("$") + 1, split.get(i).length());

                        } else if (split.get(i).contains("$") && !StringUtils.isBlank(amount1)) {
                            amount2 = split.get(i).substring(split.get(i).indexOf("$") + 1, split.get(i).length()).replaceAll(",", "");
                        }
                    }
                }
                if (!StringUtils.isBlank(amount2) || !StringUtils.isBlank(amount1)) {
                    if (StringUtils.isBlank(amount2)) {
                        list.add("$" + amount1);
                    } else if (new BigDecimal(amount1).compareTo(new BigDecimal(amount2)) > 0) {
                        list.add("$" + amount1);
                    } else {
                        list.add("$" + amount2);
                    }
                }
                if (list.size() % 4 != 0 && flagOnce && list.get(list.size() - 1).contains("$") && j + 1 != partition.size()) {
                    list.add("$0");
                }

            }
            //去除回流行
            Collections.reverse(list);
            removeList(list);
        }

    }

    /**
     * 去除纯回流数据
     *
     * @param list
     * @return
     */
    private List<String> removeList(List<String> list) {

        if (list.size() < 3) {
            list.clear();
            return list;
        } else {
            if (list.get(0).contains("$")) {
                Collections.reverse(list);
                return list;
            } else {
                list.remove(0);
                removeList(list);
            }
        }
        return list;
    }


    //校验数据
    private List<String> checkText(String s) {
        int lastIndex = 0;
        String regEx = "[\\u4e00-\\u9fa5]+"; //中文正则
        Pattern pat = Pattern.compile(regEx);
        String text = s.replace("\n", " ");
        if (text.contains("总花费")) {
            lastIndex = text.indexOf("总花费") + 3;
        } else {
            lastIndex = text.length();
        }

        String substring = text.substring(text.indexOf("-") - 3, lastIndex);
        substring = substring.trim();
//        if (substring.indexOf(" ") == 0) {
//            substring = substring.substring(1, substring.length());
//        }
        List<String> collect = Arrays.stream(substring.split(" ")).collect(Collectors.toList());

        //去除[2]  脏数据和中文
            for (int i = 0; i < collect.size(); i++) {
            if (collect.get(i).equals("[2]")) {
                collect.remove(collect.get(i));
            }
            if (pat.matcher(collect.get(i)).find() && !collect.get(i).contains("-") && !collect.get(i).contains("总花费")) {
                collect.remove(collect.get(i));
            }
        }

        return collect;
    }
}
