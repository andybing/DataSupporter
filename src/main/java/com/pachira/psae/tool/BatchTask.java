package com.pachira.psae.tool;

import java.io.Serializable;

/**
 * 批次任务类
 *
 * @author zhaoxt
 * @version 1.0.0
 * @date 2016-01-20
 */
public class BatchTask implements Serializable {
    private String batchID;     //批次id
    private int totalCount;     //本批次总任务数量
    private String date;        //时间
    private String type;        //类型

    private IndexData task;     //批次中的任务
    private IndexData subData;  //speech或者text，有且仅有一种
    private String dcHost;      //推送本批次的DC主机

    public String getBatchID(){
        return batchID;
    }

    public void setBatchID(String batchID){
        this.batchID = batchID;
    }

    public int getTotalCount(){
        return totalCount;
    }

    public void setTotalCount(int totalCount){
        this.totalCount = totalCount;
    }

    public String getDate(){
        return date;
    }

    public void setDate(String date){
        this.date = date;
    }

    public String getType(){
        return type;
    }

    public void setType(String type){
        this.type = type;
    }

    public IndexData getTask(){
        return task;
    }

    public void setTask(IndexData task){
        this.task = task;
    }

    public IndexData getSubData(){
        return subData;
    }

    public void setSubData(IndexData subData){
        this.subData = subData;
    }

    public String getDcHost() {
        return dcHost;
    }

    public void setDcHost(String dcHost) {
        this.dcHost = dcHost;
    }
}


