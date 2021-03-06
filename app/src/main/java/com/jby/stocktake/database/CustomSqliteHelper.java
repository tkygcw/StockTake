package com.jby.stocktake.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.jby.stocktake.exportFeature.category.ExportCategoryListViewObject;
import com.jby.stocktake.exportFeature.category.searchFeature.ExportCategorySearchCategoryObject;
import com.jby.stocktake.exportFeature.category.searchFeature.ExportCategorySearchSubCategoryObject;
import com.jby.stocktake.exportFeature.file.ExportFileListViewObject;
import com.jby.stocktake.exportFeature.subcategory.subcategory.CsvObject;
import com.jby.stocktake.exportFeature.subcategory.subcategory.SubCategoryObject;
import com.jby.stocktake.exportFeature.subcategory.subcategory.takeAction.TakeActionObject;
import com.jby.stocktake.others.CSVWriter;
import com.jby.stocktake.sharePreference.SharedPreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomSqliteHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Database";
    private static final int DATABASE_VERSION = 1;

    public static final String TB_FILE = "tb_file";
    public static final String TB_CATEGORY = "tb_category";
    public static final String TB_SUB_CATEGORY = "tb_sub_category";

    private static final String CREATE_TABLE_FILE = "CREATE TABLE " + TB_FILE +
            "(id INTEGER PRIMARY KEY, " +
            "file_name Text, " +
            "created_at Text )";

    private static final String CREATE_TABLE_CATEGORY = "CREATE TABLE " + TB_CATEGORY +
            "(id INTEGER PRIMARY KEY, " +
            "category_name Text, " +
            "file_id Text, " +
            "created_at Text )";

    private static final String CREATE_TABLE_SUB_CATEGORY = "CREATE TABLE " + TB_SUB_CATEGORY +
            "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "barcode Text, " +
            "item_code Text default 0," +
            "description Text default 0, " +
            "check_quantity Text default 0, " +
            "system_quantity Text default 0, " +
            "selling_price Text default 0, " +
            "cost_price Text default 0," +
            "file_id Text, " +
            "category_id Text, " +
            "priority Text, " +
            "date_create Text, " +
            "time_create Text)";

    private String timeStamp;
    private Context context;

    public CustomSqliteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE_FILE);
        sqLiteDatabase.execSQL(CREATE_TABLE_CATEGORY);
        sqLiteDatabase.execSQL(CREATE_TABLE_SUB_CATEGORY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TB_FILE);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TB_CATEGORY);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TB_SUB_CATEGORY);
        onCreate(sqLiteDatabase);
    }

    //    --------------------------------------------------------------------------file purpose------------------------------------------------------------------------
    public ArrayList<ExportFileListViewObject> fetchAll() {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<ExportFileListViewObject> results = new ArrayList<>();

        String sql = "SELECT tb_file.id, tb_file.file_name, COUNT(tb_category.id), tb_file.created_at FROM " + TB_FILE +
                " LEFT JOIN " + TB_CATEGORY +
                " ON tb_file.id = tb_category.file_id" +
                " GROUP BY tb_file.file_name" +
                " ORDER BY tb_file.id ASC";

        Cursor crs = db.rawQuery(sql, null);
        while (crs.moveToNext()) {
            results.add(new ExportFileListViewObject(crs.getString(crs.getColumnIndex("id")),
                    crs.getString(crs.getColumnIndex("file_name")),
                    String.valueOf(crs.getInt(2)),
                    crs.getString(crs.getColumnIndex("created_at"))));
        }
        db.close();
        crs.close();
        return results;
    }


    public boolean deleteFileItem(String fileID) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<String> list = new ArrayList<String>();
        String sql = "SELECT id FROM " + TB_CATEGORY + " WHERE file_id IN (" + fileID + ")";
        boolean result = false;

        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            list.add(cursor.getString(cursor.getColumnIndex("id")));
        }
        boolean deleteCategory = deleteCategory(list);
        if (deleteCategory) result = true;

        db.close();
        cursor.close();
        return result;
    }

    //---------------------------------------------------------------------------- category purpose-------------------------------------------------------------------/

    public JSONObject fetchAllCategory(String fileID, int page) {
        SQLiteDatabase db = this.getReadableDatabase();

        double start;
        double limit = 20.0;
        int totalCategoryRow = countCategoryRow(fileID);
        int page_limit = (int) Math.ceil(totalCategoryRow / limit);

        //for return json purpose
        StringBuilder sb = new StringBuilder();
        JSONObject jsonObject = null;
        String json = "";

        page_limit = (int) Math.ceil(page_limit);
        if (page <= page_limit) {
            start = (page - 1) * limit;

            String sql = "SELECT tb_category.id, tb_category.category_name, COUNT(tb_sub_category.id) FROM " + TB_CATEGORY +
                    " LEFT JOIN tb_sub_category" +
                    " ON tb_category.id = tb_sub_category.category_id" +
                    " WHERE tb_category.file_id=" + fileID +
                    " GROUP BY tb_category.category_name" +
                    " ORDER BY tb_category.id DESC" +
                    " LIMIT " + start + " , " + limit;

            Cursor crs = db.rawQuery(sql, null);


            while (crs.moveToNext()) {
                ExportCategoryListViewObject object = new ExportCategoryListViewObject(crs.getString(crs.getColumnIndex("id")),
                        crs.getString(crs.getColumnIndex("category_name")),
                        String.valueOf(crs.getInt(2)));

                sb.append(object).append(",");
            }
            crs.close();
        }
        //remove the "," from result
        if (!sb.toString().equals(""))
            json = sb.substring(0, sb.length() - 1);
        //adding the format of json to result
        json = "{\"category\":[" + json + "]}";
        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("haha", "haha: data " + jsonObject);
        return jsonObject;
    }

    private int countCategoryRow(String fileID) {
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT id FROM " + TB_CATEGORY + " WHERE file_id=" + fileID;
        Cursor cursor = db.rawQuery(sql, null);
        int cursorCount = cursor.getCount();
        cursor.close();
        return cursorCount;
    }

    public int saveCategory(String category_name, String file_id) {
        boolean categoryIsExited = checkCategoryIsExisted(category_name);
        SQLiteDatabase db = this.getWritableDatabase();
        long result = -1;
        int status;

        ContentValues contentValues = new ContentValues();
        contentValues.put("category_name", category_name);
        contentValues.put("file_id", file_id);
        contentValues.put("created_at", timeStamp);

//         new record
        if (!categoryIsExited) {
            result = db.insert(TB_CATEGORY, null, contentValues);
            status = 1;
        }
//        if existed
        else {
            status = 2;
            result = 1;
        }
//        if failed
        if (result == -1)
            status = 3;

        return status;

    }

    public int updateCategory(String category_name, String id) {
        boolean categoryIsExited = checkCategoryIsExisted(category_name);
        SQLiteDatabase db = this.getWritableDatabase();
        long result;
        int status = 3;
        timeStamp = String.valueOf(android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", new java.util.Date()));

        if (!categoryIsExited) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("category_name", category_name);

            result = db.update(TB_CATEGORY, contentValues, "id=?", new String[]{id});
            if (result != -1)
                status = 1;
        } else {
            status = 2;
        }

        return status;
    }

    public boolean deleteCategory(List ids) {
        SQLiteDatabase db = this.getWritableDatabase();
        long result;
        boolean status = false;

        String args = TextUtils.join(", ", ids);
        result = db.delete(TB_CATEGORY, "id IN (" + args + ")", null);
        if (result != -1) {
            boolean deleteSubCategory = deleteSubCategoryRelatedToCategory(ids);
            if (deleteSubCategory)
                status = true;
        }
        return status;
    }

    private boolean deleteSubCategoryRelatedToCategory(List ids) {
        SQLiteDatabase db = this.getWritableDatabase();
        long result;
        boolean status = false;

        String args = TextUtils.join(", ", ids);
        result = db.delete(TB_SUB_CATEGORY, "category_id IN (" + args + ")", null);
        if (result != -1)
            status = true;
        return status;
    }

    private boolean checkCategoryIsExisted(String category_name) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT id FROM " + TB_CATEGORY + " WHERE category_name=?";
        Cursor cursor = db.rawQuery(sql, new String[]{category_name});
        boolean status = false;

        if (cursor.getCount() > 0)
            status = true;
        cursor.close();
        return status;
    }

    public ArrayList<ExportCategorySearchSubCategoryObject> searchAllSubCategoryFromCategory(String fileID, ArrayList<ExportCategorySearchSubCategoryObject> currentArrayList, String keyword) {
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT tb_sub_category.barcode, tb_sub_category.system_quantity, tb_category.category_name," +
                " tb_category.id, tb_sub_category.date_create" +
                " FROM " + TB_SUB_CATEGORY +
                " INNER JOIN " + TB_CATEGORY +
                " ON tb_sub_category.category_id = tb_category.id" +
                " WHERE tb_category.file_id=" + fileID +
                " AND (tb_sub_category.barcode LIKE '%" + keyword + "%' OR tb_sub_category.description LIKE '%" + keyword + "%')" +
                " ORDER BY tb_sub_category.priority DESC";

        Cursor crs = db.rawQuery(sql, null);

        while (crs.moveToNext()) {
            currentArrayList.add(new ExportCategorySearchSubCategoryObject(
                    crs.getString(crs.getColumnIndex("barcode")),
                    crs.getString(crs.getColumnIndex("system_quantity")),
                    crs.getString(crs.getColumnIndex("category_name")),
                    crs.getString(crs.getColumnIndex("id")),
                    crs.getString(crs.getColumnIndex("date_create"))));
        }
        crs.close();
        db.close();

        return currentArrayList;
    }

    public ArrayList<ExportCategorySearchCategoryObject> searchAllCategoryByQuery(String fileID, ArrayList<ExportCategorySearchCategoryObject> currentArrayList, String keyword) {
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT tb_category.id, tb_category.category_name, COUNT(tb_sub_category.id) FROM "
                + TB_CATEGORY + " LEFT JOIN " + TB_SUB_CATEGORY +
                " ON tb_category.id = tb_sub_category.category_id" +
                " WHERE tb_category.file_id=" + fileID + " AND (tb_category.category_name LIKE '%" + keyword + "%')" +
                " GROUP BY tb_category.category_name" +
                " ORDER BY tb_category.id DESC";

        Cursor crs = db.rawQuery(sql, null);

        while (crs.moveToNext()) {
            currentArrayList.add(new ExportCategorySearchCategoryObject(crs.getString(crs.getColumnIndex("id")),
                    crs.getString(crs.getColumnIndex("category_name")),
                    String.valueOf(crs.getInt(2))));
        }
        crs.close();
        return currentArrayList;
    }


    //---------------------------------------------------------------------------- subcategory purpose-------------------------------------------------------------------/

    public JSONObject fetchAllSubCategory(boolean readAllFile, String category_id, String filter, String file_id, int page) {
        SQLiteDatabase db = this.getReadableDatabase();
        double start = 0;
        double limit = 20.0;
        int totalSubCategoryRow = countSubCategoryRow(readAllFile, category_id, file_id);
        int page_limit = (int) Math.ceil(totalSubCategoryRow / limit);

        //for return json purpose
        StringBuilder sb = new StringBuilder();
        JSONObject jsonObject = null;
        String json = "";

        page_limit = (int) Math.ceil(page_limit);
        if (page <= page_limit) {
            start = (page - 1) * limit;

            String sql = "SELECT * FROM " + TB_SUB_CATEGORY;
            if (!readAllFile) sql = sql + " WHERE category_id=" + category_id;
            else sql = sql + " WHERE file_id =" + file_id;

            sql = filter(filter, sql) + " ORDER BY priority DESC" + " LIMIT " + start + " , " + limit;
            Cursor crs = db.rawQuery(sql, null);
            while (crs.moveToNext()) {
                SubCategoryObject object = new SubCategoryObject(
                        crs.getString(crs.getColumnIndex("id")),
                        crs.getString(crs.getColumnIndex("barcode")),
                        crs.getString(crs.getColumnIndex("check_quantity")),
                        crs.getString(crs.getColumnIndex("system_quantity")),
                        crs.getString(crs.getColumnIndex("date_create")),
                        crs.getString(crs.getColumnIndex("time_create")));

                sb.append(object).append(",");
            }
            crs.close();
        }
        //remove the "," from result
        if (!sb.toString().equals(""))
            json = sb.substring(0, sb.length() - 1);

        //adding the format of json to result
        json = "{\"sub_category\":[" + json + "]}";
        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private String filter(String filter, String sql) {
        switch (filter) {
            case "balance":
                return sql + " AND check_quantity = CAST(system_quantity AS UNSIGNED INTEGER)";
            case "more":
                return sql + " AND check_quantity > CAST(system_quantity AS UNSIGNED INTEGER)";
            case "less":
                return sql + " AND check_quantity < CAST(system_quantity AS UNSIGNED INTEGER)";
            default:
                return sql;
        }
    }

    public int countSubCategoryRow(boolean readAllFile, String categoryID, String file_id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT category_id FROM " + TB_SUB_CATEGORY;
        if (!readAllFile) sql = sql + " WHERE category_id=" + categoryID;
        else sql = sql + " WHERE file_id =" + file_id;
        Cursor cursor = db.rawQuery(sql, null);
        int cursorCount = cursor.getCount();
        cursor.close();
        return cursorCount;
    }

    //---------------------------------------------------------------------------- subcategory save purpose-------------------------------------------------------------------/

    public long saveSubCategory(boolean readAllFile, String file_id, int count, String categoryID, String barcode, double quantity) {
        String reminder = SharedPreferenceManager.getReminder(context);
        int status = 0;
        boolean reminderStatus = false;
        if (reminder.equals("1") && count == 0 && !readAllFile) {
//            check this record existed or not
            int checkRecordAvailabilityInOthers = checkSubCategoryExistedInOther(categoryID, barcode, file_id);
//            if exist
            if (checkRecordAvailabilityInOthers > 0) {
                reminderStatus = true;
                status = 3;
            }

        }
        if (!reminderStatus) {
//            if reminder = off or record is not exist in other category then proceed to here
            return checkSubCategoryExisted(barcode, categoryID, file_id, quantity);
        }
        return status;
    }

    private int checkSubCategoryExistedInOther(String categoryID, String barcode, String file_id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT tb_sub_category.id FROM " + TB_SUB_CATEGORY +
                " INNER JOIN " + TB_CATEGORY +
                " ON tb_sub_category.category_id = tb_category.id" +
                " WHERE tb_sub_category.category_id <>?" +
                " AND tb_sub_category.barcode =?" +
                " AND tb_category.file_id =?";

        Cursor cursor = db.rawQuery(sql, new String[]{categoryID, barcode, file_id});
        int cursorCount = cursor.getCount();
        cursor.close();
        return cursorCount;
    }

    private long checkSubCategoryExisted(String barcode, String categoryID, String file_id, double quantity) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor;
        long status;

        String sql = "SELECT check_quantity, id FROM " + TB_SUB_CATEGORY;
        //check by category
        if (categoryID != null) {
            sql = sql + " WHERE barcode = ? AND category_id = ? ";
            cursor = db.rawQuery(sql, new String[]{barcode, categoryID});
            //check by all file
        } else {
            sql = sql + " WHERE barcode= ? AND file_id = ? ORDER BY id DESC";
            cursor = db.rawQuery(sql, new String[]{barcode, file_id});
        }
        //update existed record
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            double newQuantity = cursor.getInt(cursor.getColumnIndex("check_quantity")) + quantity;
            String subCategoryID = cursor.getString(cursor.getColumnIndex("id"));

            boolean updateExistedRecord = updateExistedSubCategory(subCategoryID, newQuantity);
            if (updateExistedRecord) status = 1;
            else status = 4;
        }
        //create new record
        else {
            long saveNewRecord;
            //if check by file and a new record is found then return the id
            saveNewRecord = saveNewSubCategory(categoryID, quantity, barcode, file_id);
            if (saveNewRecord == -1) status = 4;
            else if (categoryID != null) status = 1;
                //return id
            else return saveNewRecord;
        }
        cursor.close();
        return status;
    }

    private boolean updateExistedSubCategory(String subCategoryID, double newQuantity) {
        String dateCreate = String.valueOf(android.text.format.DateFormat.format("yyyy-MM-dd", new java.util.Date()));
        String timeCreate = String.valueOf(android.text.format.DateFormat.format("HH:mm:ss", new java.util.Date()));
        String priority = String.valueOf(android.text.format.DateFormat.format("yyyyMMddHHmmss", new java.util.Date()));
        SQLiteDatabase db = this.getWritableDatabase();
        long result;
        boolean status = false;

        ContentValues contentValues = new ContentValues();
        contentValues.put("check_quantity", newQuantity);
        contentValues.put("date_create", dateCreate);
        contentValues.put("time_create", timeCreate);
        contentValues.put("priority", priority);

        result = db.update(TB_SUB_CATEGORY, contentValues, "id=?", new String[]{subCategoryID});
        if (result != -1)
            status = true;
        return status;
    }

    private long saveNewSubCategory(String categoryID, double quantity, String barcode, String fileID) {
        String dateCreate = String.valueOf(android.text.format.DateFormat.format("yyyy-MM-dd", new java.util.Date()));
        String timeCreate = String.valueOf(android.text.format.DateFormat.format("HH:mm:ss", new java.util.Date()));
        String priority = String.valueOf(android.text.format.DateFormat.format("yyyyMMddHHmmss", new java.util.Date()));
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        if (categoryID != null) contentValues.put("category_id", categoryID);
        else contentValues.put("file_id", fileID);
        contentValues.put("check_quantity", quantity);
        contentValues.put("barcode", barcode);
        contentValues.put("date_create", dateCreate);
        contentValues.put("time_create", timeCreate);
        contentValues.put("priority", priority);
        return db.insert(TB_SUB_CATEGORY, null, contentValues);
    }

    public SubCategoryObject getItemDetail(String itemID) {
        SubCategoryObject object = null;
        String sql = "SELECT tb_category.category_name, tb_sub_category.barcode, tb_sub_category.item_code, tb_sub_category.check_quantity, tb_sub_category.system_quantity, tb_sub_category.cost_price, tb_sub_category.selling_price, tb_sub_category.date_create, tb_sub_category.time_create, tb_sub_category.description" +
                " FROM " + TB_SUB_CATEGORY +
                " LEFT JOIN TB_CATEGORY ON tb_category.id = tb_sub_category.category_id" +
                " WHERE tb_sub_category.id = " + itemID;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor crs = db.rawQuery(sql, null);
        while (crs.moveToNext()) {
            object = new SubCategoryObject(
                    crs.getString(crs.getColumnIndex("barcode")),
                    crs.getString(crs.getColumnIndex("item_code")),
                    crs.getString(crs.getColumnIndex("description")),
                    crs.getString(crs.getColumnIndex("check_quantity")),
                    crs.getString(crs.getColumnIndex("system_quantity")),
                    crs.getString(crs.getColumnIndex("date_create")),
                    crs.getString(crs.getColumnIndex("time_create")),
                    crs.getString(crs.getColumnIndex("category_name")),
                    crs.getString(crs.getColumnIndex("selling_price")),
                    crs.getString(crs.getColumnIndex("cost_price")));
        }
        crs.close();
        return object;
    }

    //---------------------------------------------------------------------------- subcategory update purpose-------------------------------------------------------------------/

    public int updateSubCategory(double quantity, String subCategoryID) {
        int status = 0;
        boolean update = updateRecord(quantity, subCategoryID);
        if (update) status = 1;
        else status = 2;
        return status;
    }

    private boolean updateRecord(double newQuantity, String subCategoryID) {
        SQLiteDatabase db = this.getReadableDatabase();
        String dateCreate = String.valueOf(android.text.format.DateFormat.format("yyyy-MM-dd", new java.util.Date()));
        String timeCreate = String.valueOf(android.text.format.DateFormat.format("HH:mm:ss", new java.util.Date()));
        String priority = String.valueOf(android.text.format.DateFormat.format("yyyyMMddHHmmss", new java.util.Date()));
        ContentValues contentValues = new ContentValues();
        long result;
        boolean status = false;

        contentValues.put("check_quantity", newQuantity);
        contentValues.put("date_create", dateCreate);
        contentValues.put("time_create", timeCreate);
        contentValues.put("priority", priority);

        result = db.update(TB_SUB_CATEGORY, contentValues, "id=?", new String[]{subCategoryID});
        if (result != -1) {
            status = true;
        }
        return status;
    }

    public boolean deleteSubCategory(List ids) {
        SQLiteDatabase db = this.getWritableDatabase();
        long result;
        boolean status = false;

        String args = TextUtils.join(", ", ids);
        result = db.delete(TB_SUB_CATEGORY, "id IN (" + args + ")", null);
        if (result != -1)
            status = true;
        return status;

    }

    //---------------------------------------------------------------------------- subcategory move existed record to other category purpose-------------------------------------------------------------------/
    public ArrayList<TakeActionObject> fetchAllExistedRecordFromOther(String categoryID, String barcode, String fileID, ArrayList<TakeActionObject> arrayList) {
        SQLiteDatabase db = this.getWritableDatabase();

        String sql = "SELECT tb_sub_category.id, tb_sub_category.barcode," +
                " tb_sub_category.check_quantity, tb_sub_category.date_create, tb_sub_category.time_create," +
                " tb_sub_category.category_id, tb_category.category_name FROM " + TB_SUB_CATEGORY +
                " INNER JOIN " + TB_CATEGORY +
                " ON tb_sub_category.category_id = tb_category.id" +
                " WHERE tb_sub_category.category_id<>?" +
                " AND tb_sub_category.barcode=? AND tb_category.file_id=?" +
                " ORDER BY tb_sub_category.priority DESC";

        Cursor cursor = db.rawQuery(sql, new String[]{categoryID, barcode, fileID});

        while (cursor.moveToNext()) {
            arrayList.add(new TakeActionObject(
                    cursor.getString(cursor.getColumnIndex("id")),
                    cursor.getString(cursor.getColumnIndex("barcode")),
                    cursor.getString(cursor.getColumnIndex("check_quantity")),
                    cursor.getString(cursor.getColumnIndex("date_create")),
                    cursor.getString(cursor.getColumnIndex("time_create")),
                    cursor.getString(cursor.getColumnIndex("category_id")),
                    cursor.getString(cursor.getColumnIndex("category_name"))));
        }
        cursor.close();
        db.close();
        return arrayList;

    }

    public boolean getMoveItemQuantity(String subCategoryID, double quantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean status = false;
        String sql = "SELECT check_quantity FROM " + TB_SUB_CATEGORY +
                " WHERE id=?";

        Cursor cursor = db.rawQuery(sql, new String[]{subCategoryID});
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            double newQuantity = cursor.getInt(cursor.getColumnIndex("check_quantity")) + quantity;
            boolean moveQuantity = updateMoveItemQuantity(subCategoryID, newQuantity);

            if (moveQuantity)
                status = true;

        }
        cursor.close();
        return status;
    }

    private boolean updateMoveItemQuantity(String subCategoryID, double quantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        String priority = String.valueOf(android.text.format.DateFormat.format("yyyyMMddHHmmss", new java.util.Date()));
        long result;
        boolean status = false;

        contentValues.put("check_quantity", quantity);
        contentValues.put("priority", priority);
        result = db.update(TB_SUB_CATEGORY, contentValues, "id=?", new String[]{subCategoryID});
        if (result != -1)
            status = true;
        return status;
    }

    //---------------------------------------------------------------------------- subcategory search purpose-------------------------------------------------------------------/
    public ArrayList<SubCategoryObject> searchAllSubCategoryByQuery(boolean readAllFile, String category_id, String filter, String file_id, int page, ArrayList<SubCategoryObject> currentArrayList, String keyword) {
        SQLiteDatabase db = this.getReadableDatabase();

        double start;
        double limit = 10.0;
        int totalSubCategoryRow = countSubCategoryRow(readAllFile, category_id, file_id);
        int page_limit = (int) Math.ceil(totalSubCategoryRow / limit);

        page_limit = (int) Math.ceil(page_limit);
        if (page <= page_limit) {
            start = (page - 1) * limit;

            String sql = "SELECT * FROM " + TB_SUB_CATEGORY + " WHERE ";
            if (!readAllFile) sql = sql + "category_id=" + category_id;
            else sql = sql + "file_id=" + file_id;
            sql = filter(filter, sql) + " AND (barcode LIKE '%" + keyword + "%' OR description LIKE '%" + keyword + "%')" + " ORDER BY priority DESC" + " LIMIT " + start + " , " + limit;

            Cursor crs = db.rawQuery(sql, null);


            while (crs.moveToNext()) {
                currentArrayList.add(new SubCategoryObject(
                        crs.getString(crs.getColumnIndex("id")),
                        crs.getString(crs.getColumnIndex("barcode")),
                        crs.getString(crs.getColumnIndex("check_quantity")),
                        crs.getString(crs.getColumnIndex("system_quantity")),
                        crs.getString(crs.getColumnIndex("date_create")),
                        crs.getString(crs.getColumnIndex("time_create"))));
            }
            crs.close();
        }
        db.close();

        return currentArrayList;
    }

    //---------------------------------------------------------------------------all file purpose-------------------------------------------------------------------/

    public int countAllFileQuantity(String fileID) {
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT tb_sub_category.id FROM " + TB_SUB_CATEGORY +
                " INNER JOIN " + TB_CATEGORY +
                " ON tb_sub_category.category_id = tb_category.id" +
                " WHERE tb_category.file_id =" + fileID;
        Cursor cursor = db.rawQuery(sql, null);
        int cursorCount = cursor.getCount();
        cursor.close();
        return cursorCount;
    }

    //-------------------------------------------------------------------export purpose------------------------------------------------------------------------------/
    public CSVWriter createCSVFile(String categoryID, String fileID, FileWriter file) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor;
        StringBuilder sql = new StringBuilder("SELECT ");
        ArrayList<CsvObject> columns = getColumns();
        if (columns.size() > 0) {
            for (int i = 0; i < columns.size(); i++) {
                if (i == 0)
                    sql.append(columns.get(i).getColumns().substring(1));
                else
                    sql.append(columns.get(i).getColumns());
            }
        } else {
            Toast.makeText(context, "Nothing to be export!", Toast.LENGTH_SHORT).show();
            return null;
        }

        sql.append(" FROM TB_SUB_CATEGORY INNER JOIN TB_CATEGORY ON tb_sub_category.category_id = tb_category.id");
        //export by category
        if (categoryID != null) {
            sql.append(" WHERE tb_category.id = ? ORDER BY tb_sub_category.priority DESC");
            Log.d("haha", "sql:: " + sql);
            cursor = db.rawQuery(sql.toString(), new String[]{categoryID});
        }
        //export by all file
        else {
            sql.append(" WHERE tb_category.file_id = ? ORDER BY tb_sub_category.priority DESC");
            Log.d("haha", "sql:: " + sql);
            cursor = db.rawQuery(sql.toString(), new String[]{fileID});
        }
        CSVWriter csvWrite = new CSVWriter(file);
        csvWrite.writeNext(getHeaders(columns));

        while (cursor.moveToNext()) {
            csvWrite.writeNext(returnValueSetup(cursor, columns.size()));
        }
        cursor.close();
        db.close();
        return csvWrite;
    }

    //setup columns
    private ArrayList<CsvObject> getColumns() {
        ArrayList<CsvObject> arrayList = new ArrayList<>();
        if (SharedPreferenceManager.getExportField(context, "ExportCategory").equals("1"))
            arrayList.add(new CsvObject(SharedPreferenceManager.getExportDefaultValue(context, "ExportCategoryValue"), ",tb_category.category_name"));

        if (SharedPreferenceManager.getExportField(context, "ExportItemCode").equals("1"))
            arrayList.add(new CsvObject(SharedPreferenceManager.getExportDefaultValue(context, "ExportItemCodeValue"), ",tb_sub_category.item_code"));

        if (SharedPreferenceManager.getExportField(context, "ExportDescription").equals("1"))
            arrayList.add(new CsvObject(SharedPreferenceManager.getExportDefaultValue(context, "ExportDescriptionValue"), ",tb_sub_category.description"));

        if (SharedPreferenceManager.getExportField(context, "ExportBarcode").equals("1"))
            arrayList.add(new CsvObject(SharedPreferenceManager.getExportDefaultValue(context, "ExportBarcodeValue"), ",tb_sub_category.barcode"));

        if (SharedPreferenceManager.getExportField(context, "ExportSystemQuantity").equals("1"))
            arrayList.add(new CsvObject(SharedPreferenceManager.getExportDefaultValue(context, "ExportSystemQuantityValue"), ",tb_sub_category.system_quantity"));

        if (SharedPreferenceManager.getExportField(context, "ExportCostPrice").equals("1"))
            arrayList.add(new CsvObject(SharedPreferenceManager.getExportDefaultValue(context, "ExportCostPriceValue"), ",tb_sub_category.cost_price"));

        if (SharedPreferenceManager.getExportField(context, "ExportSellingPrice").equals("1"))
            arrayList.add(new CsvObject(SharedPreferenceManager.getExportDefaultValue(context, "ExportSellingPriceValue"), ",tb_sub_category.selling_price"));

        if (SharedPreferenceManager.getExportField(context, "ExportCheckQuantity").equals("1"))
            arrayList.add(new CsvObject(SharedPreferenceManager.getExportDefaultValue(context, "ExportCheckQuantityValue"), ",tb_sub_category.check_quantity"));

        if (SharedPreferenceManager.getExportField(context, "ExportDate").equals("1"))
            arrayList.add(new CsvObject(SharedPreferenceManager.getExportDefaultValue(context, "ExportDateValue"), ",tb_sub_category.date_create"));

        if (SharedPreferenceManager.getExportField(context, "ExportTime").equals("1"))
            arrayList.add(new CsvObject(SharedPreferenceManager.getExportDefaultValue(context, "ExportTimeValue"), ",tb_sub_category.time_create"));

        return arrayList;
    }

    //setup return value
    private String[] returnValueSetup(Cursor cursor, int size) {
        ArrayList<String> returnValue = new ArrayList<String>();
        //loop each column
        for (int i = 0; i < size; i++) {
            String columnData = cursor.getString(i);
            //control check quantity format purpose (decimal or not decimal)
            if(cursor.getColumnName(i).equals("check_quantity")){
                if(SharedPreferenceManager.getQuantityDecimal(context).equals("default")){
                    try{
                        columnData = columnData.split("\\.")[0];
                    }catch (Exception e){
                    }
                }
            }
            returnValue.add(columnData);
        }
        Object[] objNames = returnValue.toArray();
        //Second Step: convert Object array to String array
        return Arrays.copyOf(objNames, objNames.length, String[].class);
    }

    //setup header
    private String[] getHeaders(ArrayList<CsvObject> arrayList) {
        String header[] = new String[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            header[i] = arrayList.get(i).getHeader();
        }
        return header;
    }

    public String fileName(String fileID) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor;
        String fileName = "";
        String sql = "SELECT file_name FROM tb_file WHERE id = ?";
        cursor = db.rawQuery(sql, new String[]{fileID});
        while (cursor.moveToNext()) {
            fileName = cursor.getString(0);
        }

        cursor.close();
        db.close();
        return fileName;
    }
}
