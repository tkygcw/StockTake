package com.jby.stocktake.shareObject;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class ApiManager {
    private String domain = "http://www.chafor.net";
    private String prefix = "/stocktake";
    //    login and registration
    public String registerID = domain + prefix + "/registration/register.php";
    public String login = domain + prefix + "/registration/login.php";
    public String forgot = domain + prefix +  "/registration/forgot_password.php";
    //    home
    public String home = domain + prefix + "/main/home.php";
    //    download
    public String download = domain + prefix + "/download/download.php";
    //    upload
    public String upload = domain + prefix + "/upload/upload.php";
    //    setting url
    public String userAccount = domain + prefix +"/setting/user_activation.php";
    //    device name url
    public String device = domain + prefix + "/registration/device.php";

    public String setData(ArrayList<ApiDataObject> apiDataObjectArrayList) {
        String apiDataPost = "";
        String anApiDataPost = "";

        for (int position = 0; position < apiDataObjectArrayList.size(); position++) {
            if (apiDataObjectArrayList.size() > 0) {
                try {
                    anApiDataPost = URLEncoder.encode(apiDataObjectArrayList.get(position).getDataKey(), "UTF-8")
                            + "=" +
                            URLEncoder.encode(apiDataObjectArrayList.get(position).getDataContent(), "UTF-8");

                    apiDataPost += anApiDataPost;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } finally {
                    if (position != (apiDataObjectArrayList.size() - 1))
                        apiDataPost += "&";
                }
            }
        }

        return apiDataPost;
    }

    /*
     *       Set Data <key>=<data> OR Model <model-name>[<key>]=<data>
     * */
    public String setModel(ArrayList<ApiModelObject> apiModelObjectArrayList) {
        String apiModelPost = "";
        String anApiDataPost = "";

        /*
         *
         *       Build Post Data In Model Format
         *
         * */
        for (int position = 0; position < apiModelObjectArrayList.size(); position++) {
            if (apiModelObjectArrayList.size() > 0) {
                try {
                    anApiDataPost = URLEncoder.encode(apiModelObjectArrayList.get(position).getModelName(), "UTF-8")
                            + URLEncoder.encode("[", "UTF-8")
                            + URLEncoder.encode(apiModelObjectArrayList.get(position).getApiDataObject().getDataKey(), "UTF-8")
                            + URLEncoder.encode("]", "UTF-8")
                            + "="
                            + URLEncoder.encode(apiModelObjectArrayList.get(position).getApiDataObject().getDataContent(), "UTF-8");

                    apiModelPost += anApiDataPost;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } finally {
                    if (position != (apiModelObjectArrayList.size() - 1))
                        apiModelPost += "&";
                }
            }
        }

        return apiModelPost;
    }

    /*
     *       List Data Or Model
     * */
    public String setListModel(String Model, ArrayList<ArrayList<ApiDataObject>> apiListModelObjectArrayList) {
        String apiListModelPost = "";

        String anApiModelPost = "";

        /*
         *
         *       Build Post Data In List Model Format
         *
         * */
        for (int position = 0; position < apiListModelObjectArrayList.size(); position++) {
            if (apiListModelObjectArrayList.size() > 0) {
                try {
                    for (int innerPosition = 0; innerPosition < apiListModelObjectArrayList.get(position).size(); innerPosition++) {
                        anApiModelPost = URLEncoder.encode(Model, "UTF-8")
                                + URLEncoder.encode("[", "UTF-8")
                                + position
                                + URLEncoder.encode("]", "UTF-8")
                                + URLEncoder.encode("[", "UTF-8")
                                + URLEncoder.encode(apiListModelObjectArrayList.get(position).get(innerPosition).getDataKey(), "UTF-8")
                                + URLEncoder.encode("]", "UTF-8")
                                + "="
                                + URLEncoder.encode(apiListModelObjectArrayList.get(position).get(innerPosition).getDataContent(), "UTF-8");

                        Log.i("Each Api", anApiModelPost);

                        apiListModelPost += anApiModelPost;
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } finally {
                    if (position != (apiListModelObjectArrayList.size() - 1))
                        apiListModelPost += "&";
                }
            }
        }

        return apiListModelPost;
    }

    /*
     *       Data OR Model OR List Model Joining
     * */
    public String getResultParameter(String data, String model, String listModel) {

        if ((!data.equals("")) && (!model.equals("")) && (!listModel.equals("")))
            return data + "&" + model + "&" + listModel;

        else if ((!data.equals("")) && (!model.equals("")) && (listModel.equals("")))
            return data + "&" + model;
        else if ((!data.equals("")) && (model.equals("")) && (!listModel.equals("")))
            return data + "&" + listModel;
        else if ((data.equals("")) && (!model.equals("")) && (!listModel.equals("")))
            return model + "&" + listModel;

        else if ((!data.equals("")) && (model.equals("")) && (listModel.equals("")))
            return data;
        else if ((data.equals("")) && (!model.equals("")) && (listModel.equals("")))
            return model;
        else if ((data.equals("")) && (model.equals("")) && (!listModel.equals("")))
            return listModel;

        else
            return "";
    }
}
