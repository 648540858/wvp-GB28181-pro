package com.genersoft.iot.vmp.service.bean;

/**
 * 收录行业编码
 */
public enum IndustryCodeTypeEnum {
    SOCIAL_SECURITY_ROAD("00", "社会治安路面接入", "包括城市路面、商业街、公共区域、重点区域"),
    SOCIAL_SECURITY_COMMUNITY("01", "社会治安社区接入", "包括社区、楼宇、网吧等"),
    SOCIAL_SECURITY__INTERNAL("02", "社会治安内部接入 ", "包括公安办公楼、留置室等"),
    SOCIAL_SECURITY_OTHER("03", "社会治安其他接入", ""),
    TRAFFIC_ROAD("04", "交通路面接入 ", "包括城市主要干道、国道、高速交通状况监视"),
    TRAFFIC_BAYONET("05", "交通卡口接入", "包括交叉路口、“电子警察”、关口、收费站等"),
    TRAFFIC_INTERNAL("06", "交通内部接入", "包括交管办公楼等"),
    TRAFFIC_OTHER("07", "交通其他接入", ""),
    CITY_MANAGEMENT("08", "城市管理接入", ""),
    HEALTH_ENVIRONMENTAL_PROTECTION("09", "卫生环保接入", ""),
    COMMODITY_INSPECTION_CUSTOMHOUSE("10", "商检海关接入", ""),
    EDUCATION_SECTOR("11", "教育部门接入", ""),
    CIVIL_AVIATION("12", "民航接入", ""),
    RAILWAY("13", "铁路接入", ""),
    SHIPPING("14", "航运接入", ""),
    AGRICULTURE_FORESTRY_ANIMAL_HUSBANDRY_FISHING("40", "农、林、牧、渔业接入", ""),
    MINING("41", "采矿业接入", ""),
    MANUFACTURING_INDUSTRY("42", "制造业接入", ""),
    ELECTRICITY_HEAT_GAS_AND_WATER_PRODUCTION_AND_SUPPLY("43", "电力、热力、燃气及水生产和供应业接入", ""),
    CONSTRUCTION("44", "建筑业接入", ""),
    WHOLESALE_AND_RETAIL("45", "批发和零售业接入", ""),
    ;

    /**
     * 接入类型码
     */
    private String name;

    /**
     * 名称
     */
    private String code;

    /**
     * 备注
     */
    private String notes;

    IndustryCodeTypeEnum(String code, String name, String notes) {
        this.name = name;
        this.code = code;
        this.notes = notes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
