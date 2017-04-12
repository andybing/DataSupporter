package com.pachira.psae.tool;

import java.io.Serializable;
import java.util.List;

/**
 * 索引数据类
 *
 * @author andy
 * @version 1.0.0
 * @date 2016-01-22
 */

public class IndexData implements Serializable {
    private List<String> nameList;
    private List<List<Object>> valueList;

    public List<String> getNameList(){
        return nameList;
    }

    public void setNameList(List<String> nameList){
        this.nameList = nameList;
    }

    public List<List<Object>> getValueList(){
        return valueList;
    }

    public void setValueList(List<List<Object>> valueList){
        this.valueList = valueList;
    }
}
