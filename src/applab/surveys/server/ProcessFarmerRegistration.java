package applab.surveys.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import applab.Crops;
import applab.Farmer;
import applab.Gender;
import applab.HouseholdStatus;
import applab.InformationTopics;
import applab.Livestock;
import applab.server.ApplabServlet;
import applab.server.ServletRequestContext;

/**
 * Servlet implementation class ProcessFarmerRegistration
 */
public class ProcessFarmerRegistration extends ApplabServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public ProcessFarmerRegistration() {
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void doApplabPost(HttpServletRequest request,
                                HttpServletResponse response, ServletRequestContext context)
            throws Exception {

        // TODO: make farmer point to CKW, use IMEI to create the link
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String farmerId = request.getParameter("farmerId");

        // create farmer object and set required fields
        Farmer farmer = Farmer.create(firstName, lastName, farmerId);
        populateRequiredFields(farmer, request);
        populateOptionalFields(farmer, request);

        String farmerSalesforceId = farmer.save();
        if (farmerSalesforceId != null) {
            response.getWriter().write("Successfully created farmer profile");
        }
        else {
            response.getWriter().write("Failed to create farmer profile");
        }
    }

    private void populateRequiredFields(Farmer farmer, HttpServletRequest request) {
        String fatherName = request.getParameter("fatherName");
        String householdStatus = request.getParameter("householdStatus");
        String gender = request.getParameter("gender");
        // String village = request.getParameter("village");

        farmer.setGender(Gender.valueOf(gender));
        farmer.setFatherName(fatherName);
        farmer.setHouseholdStatus(HouseholdStatus.valueOf(householdStatus));
        // farmer.setVillage(village); TODO: what are we doing about location?
    }

    private void populateOptionalFields(Farmer farmer, HttpServletRequest request) {
        String landSize = request.getParameter("landSize");
        String mobileNumber = request.getParameter("phoneNumber");
        String[] topCrops = request.getParameterValues("topCrops");
        String[] topLivestock = request.getParameterValues("topLivestock");
        String[] topInfoNeeds = request.getParameterValues("topInfoNeeds");

        if (landSize != null) {
            try {
                double landSizeValue = Double.parseDouble(landSize);
                farmer.setLandSize(landSizeValue);
            }
            catch (NumberFormatException e) {
                // do nothing, as this field is optional anyway
            }
        }
        if (mobileNumber != null) {
            farmer.setMobileNumber(mobileNumber);
        }
        if (topCrops != null) {
            // if more than 3 crops were provided, we only use the first 3
            for (int i = 0; i < 3 && i < topCrops.length; i++) {
                Crops crop = Crops.valueOf(topCrops[i]);
                farmer.addCrop(crop);
            }
        }
        if (topLivestock != null) {
            // if more than 2 livestock were provided, we only use the first 2
            for (int i = 0; i < 2 && i < topLivestock.length; i++) {
                Livestock livestock = Livestock.valueOf(topLivestock[i]);
                farmer.addLivestock(livestock);
            }
        }
        if (topInfoNeeds != null) {
            // if more than 3 info needs were provided, we only use the first 3
            for (int i = 0; i < 3 && i < topCrops.length; i++) {
                InformationTopics topic = InformationTopics.valueOf(topInfoNeeds[i]);
                farmer.addInfoNeed(topic);
            }
        }
    }
}
