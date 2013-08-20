package applab.surveys;

import java.text.DecimalFormat;

public class CommodityPriceKeyword {

    private String attribution;
    private String baseKeyword;   
    private String date;
    private int categoryId;
    private int id;
    private String region;
    private String district;
    private String subcounty;
    private Commodity commodity;
    private String commodityName;
    private String unit;
    private String generatedKeyword;
    private String category;

    public CommodityPriceKeyword(Commodity commodity, String attribution, String baseKeyword, int categoryId, String category) {

        this.commodity = commodity;
        this.attribution = attribution;
        this.baseKeyword = baseKeyword;
        this.categoryId = categoryId;
        this.subcounty = commodity.getSubcountyName();
        this.district = commodity.getDistrictName();
        this.region = commodity.getRegionName();
        this.commodityName = commodity.getName();
        this.category = category;
        if(commodity != null && commodity.getRetailUnitOfMeasure() != null && commodity.getRetailUnitOfMeasure() != ""){
        	this.unit = commodity.getRetailUnitOfMeasure();
        }
        else{
        	this.unit = "kg";
        }
    }

    
    public CommodityPriceKeyword(Commodity commodity, String attribution, String baseKeyword, int categoryId, String unit, String category) {

        this.commodity = commodity;
        this.attribution = attribution;
        this.baseKeyword = baseKeyword;
        this.categoryId = categoryId;
        this.subcounty = commodity.getSubcountyName();
        this.district = commodity.getDistrictName();
        this.region = commodity.getRegionName();
        this.commodityName = commodity.getName();
        this.category = category;
        if(!unit.isEmpty()){
        	this.unit = unit;
        }
        else{
        	this.unit = "kg";
        }
    }

    public CommodityPriceKeyword(String attribution, String baseKeyword, int categoryId) {

        this.attribution = attribution;
        this.baseKeyword = baseKeyword;
        this.categoryId = categoryId;
        this.unit = "kg";
    }
    public CommodityPriceKeyword() {
        this.unit = "kg";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Commodity getCommodity() {
        return commodity;
    }

    public void setCommodity(Commodity commodity) {
        this.commodity = commodity;
    }

    public String getAttribution() {
        return attribution;
    }

    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

    public String getBaseKeyword() {
        return baseKeyword;
    }

    public void setBaseKeyword(String baseKeyword) {
        this.baseKeyword = baseKeyword;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getSubcounty() {
        return subcounty;
    }

    public void setSubcounty(String subcounty) {
        this.subcounty = subcounty;
    }
    
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCommodityName() {
        return commodityName;
    }

    public void setCommodityName(String commodityName) {
        this.commodityName = commodityName;
    }
    
    public String getKeyword() {
        if (generatedKeyword == "" || generatedKeyword == null) {
            generatedKeyword = generateKeyword();
        }
        return generatedKeyword;
    }

	public void setCategory(String category) {
		this.category = category;
	}

	public String getCategory() {
		return category;
	}
	
    private String generateKeyword() {

        if (this.region == null || this.region == "" || this.district == null || this.district == "" || this.subcounty == null
                || this.subcounty == "") {
            return null;
        }
        StringBuilder keyword = new StringBuilder();
        keyword.append(this.category);
        keyword.append(" ");
        keyword.append(this.baseKeyword);
        keyword.append(" ");
        keyword.append(this.region.replace(" ", "_"));
        keyword.append(" ");
        keyword.append(this.district.replace(" ", "_"));
        keyword.append(" ");
        keyword.append(this.subcounty.replace(" ", "_"));
        keyword.append(" ");
        keyword.append(getCommodity().getMarketName().replace(" ", "_"));
        keyword.append(" ");
        keyword.append(getCommodity().getName().replace(" ", "_"));
        return keyword.toString();
    }

    public String getContent() {

        if (getCommodity().getLowRetailPrice() == 0 && getCommodity().getHighRetailPrice() == 0 && getCommodity().getLowWholesalePrice() == 0
                && getCommodity().getHighWholesalePrice() == 0) {
            return "No Content";
        }        
        return getLowRetailContent() + " \n" + getHighRetailContent() + "\n" + getLowWholesaleContent() + " \n" + getHighWholesaleContent();
        
    }

    private String getLowRetailContent() {

        String price = "";
        if (getCommodity().getLowRetailPrice() != 0) {
            price = "Lowest Retail Price: " + formatNumber(getCommodity().getLowRetailPrice()) + " " + getUnitSegment() + ".";
        }
        return price;
    }

    private String getHighRetailContent() {

        String price = "";
        if (getCommodity().getHighRetailPrice() != 0) {
            price = "Highest Retail Price: " + formatNumber(getCommodity().getLowRetailPrice()) + " " + getUnitSegment() + ".";
        }
        return price;
    }

    private String getLowWholesaleContent() {

        String price = "";
        if (getCommodity().getLowWholesalePrice() != 0) {
            price = "Lowest Wholesale Price: " + formatNumber(getCommodity().getLowWholesalePrice()) + " " + getUnitSegment() + ".";
        }
        return price;
    }

    private String getHighWholesaleContent() {

        String price = "";
        if (getCommodity().getHighWholesalePrice() != 0) {
            price = "Highest Wholesale Price: " + formatNumber(getCommodity().getHighWholesalePrice()) + " " + getUnitSegment() + ".";
        }
        return price;
    }

    private String getUnitSegment() {

        return "Shs per " + this.unit;
    }

    private String formatNumber(double number) {
        DecimalFormat formatter = new DecimalFormat("#,###,###");
        return formatter.format(number);
    }

}
