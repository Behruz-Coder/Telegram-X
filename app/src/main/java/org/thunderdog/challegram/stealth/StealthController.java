package org.thunderdog.challegram.stealth;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;
import org.thunderdog.challegram.BaseActivity;
import org.thunderdog.challegram.Log;
import org.thunderdog.challegram.tool.UI;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class StealthController {

    private static final String PREFS_NAME = "stealth_settings";

    public static final int RESULT_WRONG = 0;
    public static final int RESULT_MASTER = 1;
    public static final int RESULT_DECOY = 2;
    public static final int RESULT_PANIC = 3;

    public static final int MODE_NORMAL = 0;
    public static final int MODE_DECOY = 1;

    private static volatile StealthController instance;

    private final SharedPreferences preferences;
    private final Set<Long> allowedDialogIds = new HashSet<>();
    private final Set<Long> decoyDialogIds = new HashSet<>();

    private String masterPinHash;
    private byte[] masterPinSalt;

    private String decoyPinHash;
    private byte[] decoyPinSalt;
    private boolean decoyEnabled;

    private String panicPinHash;
    private byte[] panicPinSalt;
    private boolean panicEnabled;

    private String secretCode;
    private boolean autoLock;
    private boolean strictNotifications;
    private boolean stealthEnabled;

    private int currentMode = MODE_NORMAL;
    private boolean isUnlocked = false;

    // O'ZGARGAN: bir nechta ketma-ket notifyReload() chaqiruvini bittaga birlashtirish uchun
    private boolean reloadPending = false;

    public static StealthController getInstance() {
        StealthController local = instance;
        if (local == null) {
            synchronized (StealthController.class) {
                local = instance;
                if (local == null) {
                    instance = local = new StealthController();
                }
            }
        }
        return local;
    }

    private StealthController() {
        Context context = UI.getContext();
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadConfig();
    }

    private synchronized void loadConfig() {
        masterPinHash = preferences.getString("master_pin_hash", "");
        String saltStr = preferences.getString("master_pin_salt", "");
        masterPinSalt = TextUtils.isEmpty(saltStr) ? new byte[0] : Base64.decode(saltStr, Base64.DEFAULT);

        decoyPinHash = preferences.getString("decoy_pin_hash", "");
        String decoySaltStr = preferences.getString("decoy_pin_salt", "");
        decoyPinSalt = TextUtils.isEmpty(decoySaltStr) ? new byte[0] : Base64.decode(decoySaltStr, Base64.DEFAULT);
        decoyEnabled = preferences.getBoolean("decoy_enabled", false);

        panicPinHash = preferences.getString("panic_pin_hash", "");
        String panicSaltStr = preferences.getString("panic_pin_salt", "");
        panicPinSalt = TextUtils.isEmpty(panicSaltStr) ? new byte[0] : Base64.decode(panicSaltStr, Base64.DEFAULT);
        panicEnabled = preferences.getBoolean("panic_enabled", false);

        secretCode = preferences.getString("secret_code", "#7777#");
        autoLock = preferences.getBoolean("auto_lock", true);
        strictNotifications = preferences.getBoolean("strict_notifications", true);
        stealthEnabled = preferences.getBoolean("stealth_enabled", true);

        allowedDialogIds.clear();
        String allowedStr = preferences.getString("allowed_dialog_ids", "[]");
        try {
            JSONArray arr = new JSONArray(allowedStr);
            for (int i = 0; i < arr.length(); i++) {
                allowedDialogIds.add(arr.getLong(i));
            }
        } catch (Exception e) {
            Log.e("StealthController: failed to load allowed dialogs", e);
        }

        decoyDialogIds.clear();
        String decoyStr = preferences.getString("decoy_dialog_ids", "[]");
        try {
            JSONArray arr = new JSONArray(decoyStr);
            for (int i = 0; i < arr.length(); i++) {
                decoyDialogIds.add(arr.getLong(i));
            }
        } catch (Exception e) {
            Log.e("StealthController: failed to load decoy dialogs", e);
        }
    }

    public synchronized boolean isPasswordSet() {
        return !TextUtils.isEmpty(masterPinHash) && masterPinSalt != null && masterPinSalt.length > 0;
    }

    public synchronized boolean isStealthEnabled() {
        return stealthEnabled;
    }

    public synchronized void setStealthEnabled(boolean enabled) {
        this.stealthEnabled = enabled;
        preferences.edit().putBoolean("stealth_enabled", enabled).apply();
        notifyReload();
    }

    public synchronized boolean isAutoLock() {
        return autoLock;
    }

    public synchronized void setAutoLock(boolean autoLock) {
        this.autoLock = autoLock;
        preferences.edit().putBoolean("auto_lock", autoLock).apply();
    }

    public synchronized boolean isStrictNotifications() {
        return strictNotifications;
    }

    public synchronized void setStrictNotifications(boolean strictNotifications) {
        this.strictNotifications = strictNotifications;
        preferences.edit().putBoolean("strict_notifications", strictNotifications).apply();
    }

    public synchronized String getSecretCode() {
        return secretCode;
    }

    public synchronized void setSecretCode(String code) {
        this.secretCode = code;
        preferences.edit().putString("secret_code", code).apply();
    }

    public synchronized boolean isDecoyEnabled() {
        return decoyEnabled;
    }

    public synchronized void setDecoyEnabled(boolean enabled) {
        this.decoyEnabled = enabled;
        preferences.edit().putBoolean("decoy_enabled", enabled).apply();
    }

    public synchronized boolean isPanicEnabled() {
        return panicEnabled;
    }

    public synchronized void setPanicEnabled(boolean enabled) {
        this.panicEnabled = enabled;
        preferences.edit().putBoolean("panic_enabled", enabled).apply();
    }

    public synchronized boolean isUnlocked() {
        return isUnlocked;
    }

    public synchronized int getCurrentMode() {
        return currentMode;
    }

    public synchronized Set<Long> getAllowedDialogIds() {
        return Collections.unmodifiableSet(new HashSet<>(allowedDialogIds));
    }

    public synchronized void setAllowedDialogIds(Iterable<Long> ids) {
        allowedDialogIds.clear();
        JSONArray arr = new JSONArray();
        if (ids != null) {
            for (Long id : ids) {
                if (id != null) {
                    allowedDialogIds.add(id);
                    arr.put(id);
                }
            }
        }
        preferences.edit().putString("allowed_dialog_ids", arr.toString()).apply();
        notifyReload();
    }

    public synchronized Set<Long> getDecoyDialogIds() {
        return Collections.unmodifiableSet(new HashSet<>(decoyDialogIds));
    }

    public synchronized void setDecoyDialogIds(Iterable<Long> ids) {
        decoyDialogIds.clear();
        JSONArray arr = new JSONArray();
        if (ids != null) {
            for (Long id : ids) {
                if (id != null) {
                    decoyDialogIds.add(id);
                    arr.put(id);
                }
            }
        }
        preferences.edit().putString("decoy_dialog_ids", arr.toString()).apply();
        notifyReload();
    }

    public synchronized void setMasterPin(String pin) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        masterPinSalt = salt;
        masterPinHash = hashPin(pin, salt);

        preferences.edit()
                .putString("master_pin_hash", masterPinHash)
                .putString("master_pin_salt", Base64.encodeToString(masterPinSalt, Base64.DEFAULT))
                .apply();
        isUnlocked = true;
        currentMode = MODE_NORMAL;
    }

    public synchronized void setDecoyPin(String pin, boolean enable) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        decoyPinSalt = salt;
        decoyPinHash = hashPin(pin, salt);
        decoyEnabled = enable;

        preferences.edit()
                .putString("decoy_pin_hash", decoyPinHash)
                .putString("decoy_pin_salt", Base64.encodeToString(decoyPinSalt, Base64.DEFAULT))
                .putBoolean("decoy_enabled", enable)
                .apply();
    }

    public synchronized void setPanicPin(String pin, boolean enable) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        panicPinSalt = salt;
        panicPinHash = hashPin(pin, salt);
        panicEnabled = enable;

        preferences.edit()
                .putString("panic_pin_hash", panicPinHash)
                .putString("panic_pin_salt", Base64.encodeToString(panicPinSalt, Base64.DEFAULT))
                .putBoolean("panic_enabled", enable)
                .apply();
    }

    public synchronized int verifyPin(String pin) {
        if (TextUtils.isEmpty(pin)) {
            return RESULT_WRONG;
        }

        // Panic PIN check
        if (panicEnabled && !TextUtils.isEmpty(panicPinHash) && panicPinSalt != null && panicPinSalt.length > 0) {
            String hash = hashPin(pin, panicPinSalt);
            if (hash.equals(panicPinHash)) {
                performPanicAction();
                return RESULT_PANIC;
            }
        }

        // Master PIN check
        if (!TextUtils.isEmpty(masterPinHash) && masterPinSalt != null && masterPinSalt.length > 0) {
            String hash = hashPin(pin, masterPinSalt);
            if (hash.equals(masterPinHash)) {
                // O'ZGARGAN: holat o'zgarmagan bo'lsa, ekranni qayta yaratmaymiz
                final boolean changed = !isUnlocked || currentMode != MODE_NORMAL;
                isUnlocked = true;
                currentMode = MODE_NORMAL;
                if (changed) {
                    notifyReload();
                }
                return RESULT_MASTER;
            }
        }

        // Decoy PIN check
        if (decoyEnabled && !TextUtils.isEmpty(decoyPinHash) && decoyPinSalt != null && decoyPinSalt.length > 0) {
            String hash = hashPin(pin, decoyPinSalt);
            if (hash.equals(decoyPinHash)) {
                // O'ZGARGAN: holat o'zgarmagan bo'lsa, ekranni qayta yaratmaymiz
                final boolean changed = isUnlocked || currentMode != MODE_DECOY;
                isUnlocked = false;
                currentMode = MODE_DECOY;
                if (changed) {
                    notifyReload();
                }
                return RESULT_DECOY;
            }
        }

        return RESULT_WRONG;
    }

    public synchronized boolean isChatAllowed(long dialogId) {
        if (!stealthEnabled) {
            return true;
        }
        if (isUnlocked) {
            return true;
        }
        if (currentMode == MODE_DECOY) {
            return decoyDialogIds.contains(dialogId);
        }
        // In normal stealth locked mode, only allowedDialogIds are visible
        return allowedDialogIds.contains(dialogId);
    }

    public synchronized void lock() {
        if (autoLock) {
            // O'ZGARGAN: allaqachon qulflangan bo'lsa, ekranni qayta yaratmaymiz
            final boolean changed = isUnlocked || currentMode != MODE_NORMAL;
            isUnlocked = false;
            currentMode = MODE_NORMAL;
            if (changed) {
                notifyReload();
            }
        }
    }

    public synchronized void forceLock() {
        isUnlocked = false;
        currentMode = MODE_NORMAL;
        notifyReload();
    }

    public synchronized void performPanicAction() {
        // Clear all stealth settings and revert to clean state
        preferences.edit().clear().apply();
        allowedDialogIds.clear();
        decoyDialogIds.clear();
        masterPinHash = null;
        masterPinSalt = null;
        decoyPinHash = null;
        decoyPinSalt = null;
        panicPinHash = null;
        panicPinSalt = null;
        decoyEnabled = false;
        panicEnabled = false;
        isUnlocked = false;
        currentMode = MODE_NORMAL;
        stealthEnabled = false;
        notifyReload();
    }

    public synchronized String exportConfigJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("version", 1);
            obj.put("master_pin_hash", masterPinHash);
            obj.put("master_pin_salt", masterPinSalt != null && masterPinSalt.length > 0 ? Base64.encodeToString(masterPinSalt, Base64.DEFAULT) : "");
            obj.put("decoy_enabled", decoyEnabled);
            obj.put("decoy_pin_hash", decoyPinHash);
            obj.put("decoy_pin_salt", decoyPinSalt != null && decoyPinSalt.length > 0 ? Base64.encodeToString(decoyPinSalt, Base64.DEFAULT) : "");
            obj.put("panic_enabled", panicEnabled);
            obj.put("panic_pin_hash", panicPinHash);
            obj.put("panic_pin_salt", panicPinSalt != null && panicPinSalt.length > 0 ? Base64.encodeToString(panicPinSalt, Base64.DEFAULT) : "");
            obj.put("secret_code", secretCode);
            obj.put("auto_lock", autoLock);
            obj.put("strict_notifications", strictNotifications);
            obj.put("stealth_enabled", stealthEnabled);

            JSONArray allowedArr = new JSONArray();
            for (Long id : allowedDialogIds) {
                allowedArr.put(id);
            }
            obj.put("allowed_dialog_ids", allowedArr);

            JSONArray decoyArr = new JSONArray();
            for (Long id : decoyDialogIds) {
                decoyArr.put(id);
            }
            obj.put("decoy_dialog_ids", decoyArr);

            return obj.toString(2);
        } catch (Exception e) {
            Log.e("StealthController: failed to export config json", e);
            return null;
        }
    }

    public synchronized boolean importConfigJson(String jsonStr) {
        if (TextUtils.isEmpty(jsonStr)) return false;
        try {
            JSONObject obj = new JSONObject(jsonStr);
            if (!obj.has("version")) return false;

            SharedPreferences.Editor editor = preferences.edit();

            if (obj.has("master_pin_hash")) {
                editor.putString("master_pin_hash", obj.getString("master_pin_hash"));
            }
            if (obj.has("master_pin_salt")) {
                editor.putString("master_pin_salt", obj.getString("master_pin_salt"));
            }
            if (obj.has("decoy_enabled")) {
                editor.putBoolean("decoy_enabled", obj.getBoolean("decoy_enabled"));
            }
            if (obj.has("decoy_pin_hash")) {
                editor.putString("decoy_pin_hash", obj.getString("decoy_pin_hash"));
            }
            if (obj.has("decoy_pin_salt")) {
                editor.putString("decoy_pin_salt", obj.getString("decoy_pin_salt"));
            }
            if (obj.has("panic_enabled")) {
                editor.putBoolean("panic_enabled", obj.getBoolean("panic_enabled"));
            }
            if (obj.has("panic_pin_hash")) {
                editor.putString("panic_pin_hash", obj.getString("panic_pin_hash"));
            }
            if (obj.has("panic_pin_salt")) {
                editor.putString("panic_pin_salt", obj.getString("panic_pin_salt"));
            }
            if (obj.has("secret_code")) {
                editor.putString("secret_code", obj.getString("secret_code"));
            }
            if (obj.has("auto_lock")) {
                editor.putBoolean("auto_lock", obj.getBoolean("auto_lock"));
            }
            if (obj.has("strict_notifications")) {
                editor.putBoolean("strict_notifications", obj.getBoolean("strict_notifications"));
            }
            if (obj.has("stealth_enabled")) {
                editor.putBoolean("stealth_enabled", obj.getBoolean("stealth_enabled"));
            }
            if (obj.has("allowed_dialog_ids")) {
                editor.putString("allowed_dialog_ids", obj.getJSONArray("allowed_dialog_ids").toString());
            }
            if (obj.has("decoy_dialog_ids")) {
                editor.putString("decoy_dialog_ids", obj.getJSONArray("decoy_dialog_ids").toString());
            }

            editor.apply();
            loadConfig();
            notifyReload();
            return true;
        } catch (Exception e) {
            Log.e("StealthController: failed to import config json", e);
            return false;
        }
    }

    // O'ZGARGAN: ketma-ket chaqiruvlar bitta recreate() ga birlashadi,
    // yopilayotgan activity'ga recreate() chaqirilmaydi
    public void notifyReload() {
        synchronized (this) {
            if (reloadPending) {
                return;
            }
            reloadPending = true;
        }
        UI.post(() -> {
            synchronized (StealthController.this) {
                reloadPending = false;
            }
            BaseActivity activity = UI.getUiContext();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                activity.recreate();
            }
        });
    }

    private String hashPin(String pin, byte[] salt) {
        try {
            byte[] pinBytes = pin.getBytes(StandardCharsets.UTF_8);
            byte[] combined = new byte[salt.length + pinBytes.length + salt.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(pinBytes, 0, combined, salt.length, pinBytes.length);
            System.arraycopy(salt, 0, combined, salt.length + pinBytes.length, salt.length);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(combined);
            return bytesToHex(digest);
        } catch (Exception e) {
            Log.e("StealthController: hash error", e);
            return "";
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
