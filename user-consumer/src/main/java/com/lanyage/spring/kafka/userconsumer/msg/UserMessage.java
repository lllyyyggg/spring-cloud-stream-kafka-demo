package com.lanyage.spring.kafka.userconsumer.msg;


public class UserMessage {

    public static final String MA_UPDATE="update";
    public static final String MA_DELETE="delete";

    private String action;      //动作
    private Integer itemCode;    //对应的元素

    /* constructor */

    public UserMessage(){}

    public UserMessage(String action, Integer itemCode) {
        this.action = action;
        this.itemCode = itemCode;
    }

    /* toString */

    @Override
    public String toString() {
        return "UserMessage{" +
                "action='" + action + '\'' +
                ", itemCode='" + itemCode + '\'' +
                '}';
    }

    /* getters and setters */

    public static String getMaUpdate() {
        return MA_UPDATE;
    }

    public static String getMaDelete() {
        return MA_DELETE;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getItemCode() {
        return itemCode;
    }

    public void setItemCode(Integer itemCode) {
        this.itemCode = itemCode;
    }
}
