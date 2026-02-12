package com.snuabar.mycomfy.main.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class DB {
    private static final String TAG = DB.class.getName();

    // 数据库信息
    private static String DATABASE_NAME = null;
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME_MAIN = "main_table";
    private static final String TABLE_NAME_MESSAGE_ID = "message_id_table";

    // 列名
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_KEY = "_key";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_VALUE = "value";

    // 数据类型常量
    private static final int TYPE_STRING = 0;
    private static final int TYPE_INT = 1;
    private static final int TYPE_LONG = 2;
    private static final int TYPE_FLOAT = 3;
    private static final int TYPE_DOUBLE = 4;
    private static final int TYPE_BOOLEAN = 5;
    private static final int TYPE_BYTE_ARRAY = 6;
    private static final int TYPE_STRING_LIST = 7;
    private static final int TYPE_STRING_SET = 8;
    private static final int TYPE_INT_ARRAY = 9;
    private static final int TYPE_LONG_ARRAY = 10;
    private static final int TYPE_FLOAT_ARRAY = 11;
    private static final int TYPE_DOUBLE_ARRAY = 12;
    private static final int TYPE_BOOLEAN_ARRAY = 13;
    private static final int TYPE_SERIALIZABLE = 14;

    // 数据库相关
    private final SQLiteDatabase database;
    private final DatabaseHelper dbHelper;
    private final Context context;

    /**
     * 私有构造函数
     */
    DB(Context context, String dbFilePath) {
        this.context = context;
        DATABASE_NAME = dbFilePath == null ? this.context.getPackageName() + ".db" : dbFilePath;
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
    }

    /**
     * 关闭数据库
     */
    public synchronized void close() {
        if (database != null && database.isOpen()) {
            database.close();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    public synchronized boolean isOpened() {
        return database != null && database.isOpen();
    }
    // ==================== 写入方法 ====================

    /**
     * 存储字符串值
     */
    public synchronized boolean putString(String key, String value) {
        return putValue(key, value, TYPE_STRING);
    }

    /**
     * 存储整数值
     */
    public synchronized boolean putInt(String key, int value) {
        return putValue(key, String.valueOf(value), TYPE_INT);
    }

    /**
     * 存储长整数值
     */
    public synchronized boolean putLong(String key, long value) {
        return putValue(key, String.valueOf(value), TYPE_LONG);
    }

    /**
     * 存储浮点数值
     */
    public synchronized boolean putFloat(String key, float value) {
        return putValue(key, String.valueOf(value), TYPE_FLOAT);
    }

    /**
     * 存储双精度值
     */
    public synchronized boolean putDouble(String key, double value) {
        return putValue(key, String.valueOf(value), TYPE_DOUBLE);
    }

    /**
     * 存储布尔值
     */
    public synchronized boolean putBoolean(String key, boolean value) {
        return putValue(key, value ? "1" : "0", TYPE_BOOLEAN);
    }

    /**
     * 存储字节数组
     */
    public synchronized boolean putByteArray(String key, byte[] value) {
        if (value == null) {
            return remove(key);
        }
        return putValue(key, bytesToHex(value), TYPE_BYTE_ARRAY);
    }

    /**
     * 存储字符串列表
     */
    public synchronized boolean putStringList(String key, List<String> value) {
        if (value == null) {
            return remove(key);
        }
        JSONArray jsonArray = new JSONArray();
        for (String item : value) {
            jsonArray.put(item);
        }
        return putValue(key, jsonArray.toString(), TYPE_STRING_LIST);
    }

    /**
     * 存储字符串集合
     */
    public synchronized boolean putStringSet(String key, Set<String> value) {
        if (value == null) {
            return remove(key);
        }
        JSONArray jsonArray = new JSONArray();
        for (String item : value) {
            jsonArray.put(item);
        }
        return putValue(key, jsonArray.toString(), TYPE_STRING_SET);
    }

    /**
     * 存储整型数组
     */
    public synchronized boolean putIntArray(String key, int[] value) {
        if (value == null) {
            return remove(key);
        }
        JSONArray jsonArray = new JSONArray();
        for (int item : value) {
            jsonArray.put(item);
        }
        return putValue(key, jsonArray.toString(), TYPE_INT_ARRAY);
    }

    /**
     * 存储长整型数组
     */
    public synchronized boolean putLongArray(String key, long[] value) {
        if (value == null) {
            return remove(key);
        }
        JSONArray jsonArray = new JSONArray();
        for (long item : value) {
            jsonArray.put(item);
        }
        return putValue(key, jsonArray.toString(), TYPE_LONG_ARRAY);
    }

    /**
     * 存储浮点数组
     */
    public synchronized boolean putFloatArray(String key, float[] value) throws JSONException {
        if (value == null) {
            return remove(key);
        }
        JSONArray jsonArray = new JSONArray();
        for (float item : value) {
            jsonArray.put(item);
        }
        return putValue(key, jsonArray.toString(), TYPE_FLOAT_ARRAY);
    }

    /**
     * 存储双精度数组
     */
    public synchronized boolean putDoubleArray(String key, double[] value) throws JSONException {
        if (value == null) {
            return remove(key);
        }
        JSONArray jsonArray = new JSONArray();
        for (double item : value) {
            jsonArray.put(item);
        }
        return putValue(key, jsonArray.toString(), TYPE_DOUBLE_ARRAY);
    }

    /**
     * 存储布尔数组
     */
    public synchronized boolean putBooleanArray(String key, boolean[] value) {
        if (value == null) {
            return remove(key);
        }
        JSONArray jsonArray = new JSONArray();
        for (boolean item : value) {
            jsonArray.put(item ? 1 : 0);
        }
        return putValue(key, jsonArray.toString(), TYPE_BOOLEAN_ARRAY);
    }

    /**
     * 存储可序列化对象
     */
    public synchronized boolean putSerializable(String key, Object value) {
        if (value == null) {
            return remove(key);
        }

        if (!(value instanceof java.io.Serializable)) {
            Log.e(TAG, "Object must implement Serializable");
            return false;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(value);
            oos.close();
            byte[] bytes = baos.toByteArray();
            return putValue(key, bytesToHex(bytes), TYPE_SERIALIZABLE);
        } catch (IOException e) {
            Log.e(TAG, "Failed to serialize object", e);
            return false;
        }
    }

    // ==================== 读取方法 ====================

    /**
     * 读取字符串值
     */
    public synchronized String getString(String key, String defaultValue) {
        ValueEntry entry = getValueEntry(key);
        if (entry != null && entry.type == TYPE_STRING) {
            return entry.value;
        }
        return defaultValue;
    }

    /**
     * 读取整数值
     */
    public synchronized int getInt(String key, int defaultValue) {
        ValueEntry entry = getValueEntry(key);
        if (entry != null && entry.type == TYPE_INT) {
            try {
                return Integer.parseInt(entry.value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse int value for key: " + key, e);
            }
        }
        return defaultValue;
    }

    /**
     * 读取长整数值
     */
    public synchronized long getLong(String key, long defaultValue) {
        ValueEntry entry = getValueEntry(key);
        if (entry != null && entry.type == TYPE_LONG) {
            try {
                return Long.parseLong(entry.value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse long value for key: " + key, e);
            }
        }
        return defaultValue;
    }

    /**
     * 读取浮点数值
     */
    public synchronized float getFloat(String key, float defaultValue) {
        ValueEntry entry = getValueEntry(key);
        if (entry != null && entry.type == TYPE_FLOAT) {
            try {
                return Float.parseFloat(entry.value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse float value for key: " + key, e);
            }
        }
        return defaultValue;
    }

    /**
     * 读取双精度值
     */
    public synchronized double getDouble(String key, double defaultValue) {
        ValueEntry entry = getValueEntry(key);
        if (entry != null && entry.type == TYPE_DOUBLE) {
            try {
                return Double.parseDouble(entry.value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse double value for key: " + key, e);
            }
        }
        return defaultValue;
    }

    /**
     * 读取布尔值
     */
    public synchronized boolean getBoolean(String key, boolean defaultValue) {
        ValueEntry entry = getValueEntry(key);
        if (entry != null && entry.type == TYPE_BOOLEAN) {
            return "1".equals(entry.value);
        }
        return defaultValue;
    }

    /**
     * 读取字节数组
     */
    public synchronized byte[] getByteArray(String key, byte[] defaultValue) {
        ValueEntry entry = getValueEntry(key);
        if (entry != null && entry.type == TYPE_BYTE_ARRAY) {
            return hexToBytes(entry.value);
        }
        return defaultValue;
    }

    /**
     * 读取字符串列表
     */
    public synchronized List<String> getStringList(String key, List<String> defaultValue) {
        ValueEntry entry = getValueEntry(key);
        if (entry != null && entry.type == TYPE_STRING_LIST) {
            try {
                JSONArray jsonArray = new JSONArray(entry.value);
                List<String> list = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    list.add(jsonArray.getString(i));
                }
                return list;
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse string list for key: " + key, e);
            }
        }
        return defaultValue;
    }

    /**
     * 读取字符串集合
     */
    public synchronized Set<String> getStringSet(String key, Set<String> defaultValue) {
        ValueEntry entry = getValueEntry(key);
        if (entry != null && entry.type == TYPE_STRING_SET) {
            try {
                JSONArray jsonArray = new JSONArray(entry.value);
                Set<String> set = new HashSet<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    set.add(jsonArray.getString(i));
                }
                return set;
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse string set for key: " + key, e);
            }
        }
        return defaultValue;
    }

    /**
     * 读取整型数组
     */
    public synchronized int[] getIntArray(String key, int[] defaultValue) {
        ValueEntry entry = getValueEntry(key);
        if (entry != null && entry.type == TYPE_INT_ARRAY) {
            try {
                JSONArray jsonArray = new JSONArray(entry.value);
                int[] array = new int[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    array[i] = jsonArray.getInt(i);
                }
                return array;
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse int array for key: " + key, e);
            }
        }
        return defaultValue;
    }

    /**
     * 读取长整型数组
     */
    public synchronized long[] getLongArray(String key, long[] defaultValue) {
        ValueEntry entry = getValueEntry(key);
        if (entry != null && entry.type == TYPE_LONG_ARRAY) {
            try {
                JSONArray jsonArray = new JSONArray(entry.value);
                long[] array = new long[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    array[i] = jsonArray.getLong(i);
                }
                return array;
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse long array for key: " + key, e);
            }
        }
        return defaultValue;
    }

    /**
     * 读取浮点数组
     */
    public synchronized float[] getFloatArray(String key, float[] defaultValue) {
        ValueEntry entry = getValueEntry(key);
        if (entry != null && entry.type == TYPE_FLOAT_ARRAY) {
            try {
                JSONArray jsonArray = new JSONArray(entry.value);
                float[] array = new float[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    array[i] = (float) jsonArray.getDouble(i);
                }
                return array;
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse float array for key: " + key, e);
            }
        }
        return defaultValue;
    }

    /**
     * 读取双精度数组
     */
    public synchronized double[] getDoubleArray(String key, double[] defaultValue) {
        ValueEntry entry = getValueEntry(key);
        if (entry != null && entry.type == TYPE_DOUBLE_ARRAY) {
            try {
                JSONArray jsonArray = new JSONArray(entry.value);
                double[] array = new double[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    array[i] = jsonArray.getDouble(i);
                }
                return array;
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse double array for key: " + key, e);
            }
        }
        return defaultValue;
    }

    /**
     * 读取布尔数组
     */
    public synchronized boolean[] getBooleanArray(String key, boolean[] defaultValue) {
        ValueEntry entry = getValueEntry(key);
        if (entry != null && entry.type == TYPE_BOOLEAN_ARRAY) {
            try {
                JSONArray jsonArray = new JSONArray(entry.value);
                boolean[] array = new boolean[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    array[i] = jsonArray.getInt(i) == 1;
                }
                return array;
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse boolean array for key: " + key, e);
            }
        }
        return defaultValue;
    }

    /**
     * 读取可序列化对象
     */
    public synchronized Object getSerializable(String key, Object defaultValue) {
        ValueEntry entry = getValueEntry(key);
        if (entry != null && entry.type == TYPE_SERIALIZABLE) {
            try {
                byte[] bytes = hexToBytes(entry.value);
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bais);
                Object obj = ois.readObject();
                ois.close();
                return obj;
            } catch (IOException | ClassNotFoundException e) {
                Log.e(TAG, "Failed to deserialize object for key: " + key, e);
            }
        }
        return defaultValue;
    }

    // ==================== 其他操作 ====================

    /**
     * 删除指定键值
     */
    public synchronized boolean remove(String key) {
        try {
            int rows = database.delete(TABLE_NAME_MAIN, COLUMN_KEY + " = ?", new String[]{key});
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove key: " + key, e);
            return false;
        }
    }

    /**
     * 清空所有数据
     */
    public synchronized void clear() {
        try {
            database.delete(TABLE_NAME_MAIN, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear database", e);
        }
    }

    /**
     * 检查是否包含指定键
     */
    public synchronized boolean contains(String key) {
        try (Cursor cursor = database.query(TABLE_NAME_MAIN,
                new String[]{COLUMN_KEY},
                COLUMN_KEY + " = ?",
                new String[]{key},
                null, null, null)) {
            return cursor.getCount() > 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to check if key exists: " + key, e);
            return false;
        }
    }

    //region message id 专用

    public synchronized boolean putMessage(String modelId, String jsonString) {
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_KEY, modelId);
            values.put(COLUMN_VALUE, jsonString);

            // 先删除已存在的记录
            database.delete(TABLE_NAME_MESSAGE_ID, COLUMN_KEY + " = ?", new String[]{modelId});

            // 插入新记录
            long result = database.insert(TABLE_NAME_MESSAGE_ID, null, values);
            return result != -1;
        } catch (Exception e) {
            Log.e(TAG, "Failed to put value for key: " + modelId, e);
            return false;
        }
    }

    public synchronized List<String> getAllMessages() {
        List<String> messages = new ArrayList<>();
        try (Cursor cursor = database.query(TABLE_NAME_MESSAGE_ID,
                new String[]{COLUMN_VALUE},
                null,
                null,
                null, null, null)) {

            if (cursor.moveToFirst()) {
                do {
                    String value = cursor.getString(0);
                    messages.add(value);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to getAllMessages", e);
        }
        return messages;
    }

    public synchronized String getMessageById(String messageId) {
        try (Cursor cursor = database.query(TABLE_NAME_MESSAGE_ID,
                new String[]{COLUMN_KEY, COLUMN_VALUE},
                COLUMN_KEY + " = ?",
                new String[]{messageId},
                null, null, null)) {

            if (cursor.moveToFirst()) {
                return cursor.getString(1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to getAllMessageById", e);
        }
        return null;
    }

    /**
     * 删除指定键值
     */
    public synchronized boolean removeMessageById(String messageId) {
        try {
            int rows = database.delete(TABLE_NAME_MESSAGE_ID, COLUMN_KEY + " = ?", new String[]{messageId});
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove message: " + messageId, e);
            return false;
        }
    }
    //endregion

    // ==================== 私有方法 ====================

    /**
     * 存储值到数据库
     */
    private boolean putValue(String key, String value, int type) {
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_KEY, key);
            values.put(COLUMN_TYPE, type);
            values.put(COLUMN_VALUE, value);

            // 先删除已存在的记录
            database.delete(TABLE_NAME_MAIN, COLUMN_KEY + " = ?", new String[]{key});

            // 插入新记录
            long result = database.insert(TABLE_NAME_MAIN, null, values);
            return result != -1;
        } catch (Exception e) {
            Log.e(TAG, "Failed to put value for key: " + key, e);
            return false;
        }
    }

    /**
     * 从数据库读取值
     */
    private ValueEntry getValueEntry(String key) {
        try (Cursor cursor = database.query(TABLE_NAME_MAIN,
                new String[]{COLUMN_VALUE, COLUMN_TYPE},
                COLUMN_KEY + " = ?",
                new String[]{key},
                null, null, null)) {

            if (cursor.moveToFirst()) {
                String value = cursor.getString(0);
                int type = cursor.getInt(1);
                return new ValueEntry(value, type);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get value for key: " + key, e);
        }
        return null;
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 十六进制字符串转字节数组
     */
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    // ==================== 内部类 ====================

    /**
     * 数据库助手类
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_MAIN + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_KEY + " TEXT UNIQUE NOT NULL, " +
                    COLUMN_TYPE + " INTEGER NOT NULL, " +
                    COLUMN_VALUE + " TEXT)";
            db.execSQL(createTable);

            // 创建索引以提高查询性能
            String createIndex = "CREATE INDEX IF NOT EXISTS idx_key ON " +
                    TABLE_NAME_MAIN + " (" + COLUMN_KEY + ")";
            db.execSQL(createIndex);

            String createMessageIdTable = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_MESSAGE_ID + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_KEY + " TEXT UNIQUE NOT NULL, " +
                    COLUMN_VALUE + " TEXT)";
            db.execSQL(createMessageIdTable);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_MAIN);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_MESSAGE_ID);
            onCreate(db);
        }
    }

    /**
     * 值条目类
     */
    private static class ValueEntry {
        String value;
        int type;

        ValueEntry(String value, int type) {
            this.value = value;
            this.type = type;
        }
    }
}
