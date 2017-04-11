package com.pachira.psae.tool;

import com.pachira.psae.common.CollectionUtils;
import com.pachira.psae.common.HttpRequestUtils;
import com.pachira.psae.common.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

/**
 * 索引回写相关
 */
public class IndexBackWriter {
    private static final String PSAE_CONTEXT = "/PSAE/rest/v1";
    private static final String USER_MODELER = "modeler";
    private static final String USER_PASSWORD = "000000";
    public static void main(String[] args) {
        //询问PSAE1和PSAE2的地址
        Scanner sc = new Scanner(System.in);
        System.out.print("请输入PSAE1安装的IP地址: ");
        String psae_ip_1 = sc.next();
        System.out.print("请输入PSAE1的PORT端口号: ");
        String psae_port_1 = sc.next();
        System.out.print("请输入PSAE1的数据来源DC的ip: ");
        String psae_dc_ip = sc.next();
//        String psae_dc_ip = "172.21.13.135";
//        String psae_dc_port = "8182";
        System.out.print("请输入PSAE1的数据来源DC的端口号: ");
        String psae_dc_port = sc.next();
        System.out.print("请输入PSAE2的IP地址: ");
        String psae_ip_2 = sc.next();
        System.out.print("请输入PSAE2的端口号: ");
        String psae_port_2 = sc.next();
        System.out.print("请输入要获取的索引数据的任务id: ");
        String taskId = sc.next();
//        String taskId = "20170410-FMUTE10";

        //获取userToken
        String userToken = loginForUserToken(psae_ip_1, psae_port_1);
//        String userToken = loginForUserToken("172.21.13.135", "8080");

        //获取任务字段
        List taskDimensionList = queryForDimensions(psae_ip_1, psae_port_1, userToken, "task");
//        List taskDimensionList = queryForDimensions("172.21.13.135", "8080", userToken, "task");

        //获取语音字段
        List speechDimensionList = queryForDimensions(psae_ip_1, psae_port_1, userToken, "speech");
//        List speechDimensionList = queryForDimensions("172.21.13.135", "8080", userToken, "speech");

        //生成attachTaskFields
        StringBuilder attachTaskFields = new StringBuilder();
        for(Object taskField : taskDimensionList) {
            if ("ID".equals(((Map)taskField).get("id").toString())) {
                attachTaskFields.append("@TaskID@,");
            }else {
                attachTaskFields.append(((Map) taskField).get("id").toString() + ",");
            }
        }

        //生成attachSpeechFields
        StringBuilder attachSpeechFields = new StringBuilder();
        for(Object taskField : speechDimensionList) {
            if ("@ID@".equals(((Map)taskField).get("id").toString())) {
                attachSpeechFields.append("@SpeechID@,");
            }else {
                attachSpeechFields.append(((Map) taskField).get("id").toString() + ",");
            }
        }

        //获取索引数据
        List taskList = queryForIndexData(psae_ip_1, psae_port_1, taskId, userToken, attachTaskFields, attachSpeechFields) ;
//        List taskList = queryForIndexData("172.21.13.135", "8080", taskId, userToken, attachTaskFields, attachSpeechFields);

        //新建字段生成
        if (CollectionUtils.isAbsEmpty(taskList)) {
            System.out.println("task list is empty...");
            System.exit(0);
        }

        IndexData taskIndexData = new IndexData();
        IndexData speechIndexData = new IndexData();
        List<String> taskNameList = new ArrayList<String>();
        List<String> speechNameList = new ArrayList<String>();

        for (Object dimension : taskDimensionList) {
            taskNameList.add(String.valueOf(((Map) dimension).get("rawName")));
        }
        //新字段:newTask
        taskNameList.add("newTask");

        for (Object dimension : speechDimensionList) {
            speechNameList.add(String.valueOf(((Map) dimension).get("rawName")));
        }

        List<List<Object>> taskValueList = new ArrayList<List<Object>>();
        List<List<Object>> speechValueList = new ArrayList<List<Object>>();
        for (Object task : taskList) {
            Map taskMap = (Map) task;
            List<Object> _value = new ArrayList<Object>();
            for (Object dimension : taskDimensionList) {
                if ("@ID@".equals(String.valueOf(((Map) dimension).get("rawName")))) {
                    _value.add(taskMap.get("@TaskID@"));
                }else {
                    _value.add(taskMap.get(String.valueOf(((Map) dimension).get("id"))));
                }
            }
            _value.add(StringUtils.randomString(6));

            taskValueList.add(_value);

            List speechList = (List) taskMap.get("speechList");
            for (Object speech : speechList) {
                List<Object> _speechValue = new ArrayList<Object>();
                Map _speech = (Map)speech;
                for (Object dimension : speechDimensionList) {
                    if ("@ID@".equals(String.valueOf(((Map) dimension).get("rawName")))) {
                        _speechValue.add(_speech.get("@SpeechID@"));
                    }else {
                        _speechValue.add(_speech.get(String.valueOf(((Map) dimension).get("id"))));
                    }
                }

                //TextContent的设置
                int indexOfTextContent = speechNameList.indexOf("@TextContent@");
                List<Map> textList = (List) ((Map)speech).get("recognizeResult");
                List<Map> _textList = new ArrayList<Map>();
                for (Map text : textList) {
                    Map _text = new HashMap();
                    if(text.containsKey("A")) {
                        _text.put("role", "A");
                        _text.put("text", ((Map)text.get("A")).get("text"));
                        _text.put("time", ((Map)text.get("A")).get("time"));
                    }else if (text.containsKey("B")) {
                        _text.put("role", "B");
                        _text.put("text", ((Map)text.get("B")).get("text"));
                        _text.put("time", ((Map)text.get("B")).get("time"));
                    }

                    _textList.add(_text);
                }

                _speechValue.set(indexOfTextContent, _textList);
                speechValueList.add(_speechValue);
            }
        }
        taskIndexData.setNameList(taskNameList);
        taskIndexData.setValueList(taskValueList);
        speechIndexData.setNameList(speechNameList);
        speechIndexData.setValueList(speechValueList);

        //索引回写
        BatchTask batchTask = new BatchTask();
        batchTask.setBatchID(taskId.substring(0, 8) + "-" + StringUtils.randomString(6));
        batchTask.setDate(StringUtils.formatDateInFormat(
                StringUtils.parseDateInFormat(taskId.substring(0, 8), "yyyyMMdd"), "yyyy-MM-dd"));
        batchTask.setDcHost(psae_dc_ip);
        batchTask.setTask(taskIndexData);
        batchTask.setSubData(speechIndexData);
        batchTask.setTotalCount(taskList.size());
        batchTask.setType("speech");

        Map result = indexBackWrite(psae_ip_2, psae_port_2, batchTask);
//        Map result = indexBackWrite("172.22.144.33", "8080", batchTask);
        System.out.println(result.toString());
    }

    /**
     * 登录请求modeler用户token
     * @return
     */
    private static String loginForUserToken (String ip, String port) {
        try{
            String url = "http://" + ip + ":" + port + PSAE_CONTEXT + "/user/" + USER_MODELER + "/login";
            Map param = new HashMap();
            param.put("passWord", USER_PASSWORD);
            Map result = HttpRequestUtils.requestForObject(RequestMethod.GET, url, param, Map.class);
            if (result == null || StringUtils.isAbsEmpty(result.get("functionResult").toString())) {
                System.out.println("can not connect to PSAE1");
                System.exit(0);
            }

            if (result.get("userToken") == null
                    || StringUtils.isAbsEmpty(result.get("userToken").toString())) {
                System.out.println("user is not exist in PSAE1");
            }

            return result.get("userToken").toString();
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 登录请求modeler用户token
     * @return
     */
    private static List queryForDimensions (String ip, String port, String userToken, String type) {
        try{
            String url = "http://" + ip + ":" + port + PSAE_CONTEXT + "/" + type + "/dimensions";
            Map param = new HashMap();
            param.put("userToken", userToken);
            Map result = HttpRequestUtils.requestForObject(RequestMethod.GET, url, param, Map.class);
            if (result == null || StringUtils.isAbsEmpty(result.get("functionResult").toString())) {
                System.out.println("can not connect to PSAE1");
                System.exit(0);
            }

            return (List) result.get("dimensionList");
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 请求查询索引数据
     * @param ip    PSAE地址
     * @param port  PSAE端口
     * @param attachTaskFields
     *@param attachSpeechFields @return
     */
    private static List queryForIndexData (String ip, String port, String taskId,
                                           String userToken, StringBuilder attachTaskFields, StringBuilder attachSpeechFields) {
        try{
            String url = "http://" + ip + ":" + port + PSAE_CONTEXT + "/analyzeResults/task/" + taskId;
            Map param = new HashMap();
            param.put("userToken", userToken);
            param.put("attachTaskFields", attachTaskFields);
            param.put("attachSpeechFields", "@SpeechID@,recognizeResult,SpeechPath");
            param.put("pageSize", 10);
            Map result = HttpRequestUtils.requestForObject(RequestMethod.GET, url, param, Map.class);
            if (result == null || StringUtils.isAbsEmpty(result.get("functionResult").toString())) {
                System.out.println("can not connect to PSAE1");
                System.exit(0);
            }

            if (result.get("totalCount") == null || Integer.parseInt(result.get("totalCount").toString()) < 1) {
                System.out.println("user is not exist in PSAE1");
            }

            return (List) result.get("taskList");
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 索引回写
     * @param ip
     * @param port
     * @param batchTask
     * @return
     */
    private static Map indexBackWrite(String ip, String port, BatchTask batchTask) {
        try{
            String url = "http://" + ip + ":" + port + "/tasks";
            Map param = new HashMap();
            param.put("batchTask", batchTask);
            Map result = HttpRequestUtils.requestForObject(RequestMethod.POST, url, batchTask, Map.class);
            if (result == null || StringUtils.isAbsEmpty(result.get("functionResult").toString())) {
                System.out.println("can not connect to PSAE2");
                System.exit(0);
            }

            return result;
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
