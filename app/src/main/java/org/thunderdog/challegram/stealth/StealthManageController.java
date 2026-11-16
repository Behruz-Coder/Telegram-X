package org.thunderdog.challegram.stealth;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.thunderdog.challegram.R;
import org.thunderdog.challegram.navigation.HeaderButton;
import org.thunderdog.challegram.navigation.HeaderView;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.ui.SelectChatsController;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class StealthManageController extends ViewController<Void> {

    private TextView allowedChatsSubtitle;
    private TextView decoyChatsSubtitle;
    private TextView secretCodeSubtitle;
    private CheckBox stealthEnabledCheckbox;
    private CheckBox decoyEnabledCheckbox;
    private CheckBox autoLockCheckbox;
    private CheckBox strictNotifCheckbox;

    public StealthManageController() {
        super();
    }

    @Override
    protected int getHeaderTitle() {
        return 0;
    }

    @Override
    public View getCustomHeaderCell() {
        return null;
    }

    @Override
    public boolean hasCustomHeader() {
        return false;
    }

    @Override
    protected View onCreateView(Context context) {
        HeaderView header = getHeaderView();
        if (header != null) {
            header.setTitle("Yashirin Boshqaruv");
            header.addBackButton(this);
            HeaderButton lockBtn = header.addButton(R.id.menu_btn_lock, R.drawable.baseline_lock_24, 49f, this, ColorId.headerIcon);
            if (lockBtn != null) {
                lockBtn.setOnClickListener(v -> {
                    StealthController.getInstance().forceLock();
                    Toast.makeText(context, "Ilova qulflandi", Toast.LENGTH_SHORT).show();
                    navigateBack();
                });
            }
        }

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(ColorId.windowBackgroundWhite));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        root.addView(scrollView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = Screen.dp(16);
        container.setPadding(pad, pad, pad, pad * 2);
        scrollView.addView(container, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        StealthController controller = StealthController.getInstance();

        // --- 1-BO'LIM: Asosiy rejim va Chatlar ---
        addSectionHeader(container, "Chatlar Ko'rinishi va Filtrlash");

        stealthEnabledCheckbox = addCheckboxItem(container, "Yashirin rejim faol", controller.isStealthEnabled(), (buttonView, isChecked) -> {
            controller.setStealthEnabled(isChecked);
        });

        allowedChatsSubtitle = addClickableItem(container, "Ko'rinadigan chatlarni tanlash", "", v -> openChatsSelector(false));
        updateAllowedChatsText();

        addInfoText(container, "Ilova faqat shu ro'yxatdagi chatlar, guruhlar va kanallarni ko'rsatadi. Qolgan barcha chatlar yashiriladi.");

        // --- 2-BO'LIM: Soxta Rejim (Decoy Mode) ---
        addSectionHeader(container, "Soxta Rejim (Decoy Mode)");

        decoyEnabledCheckbox = addCheckboxItem(container, "Soxta parolni yoqish", controller.isDecoyEnabled(), (buttonView, isChecked) -> {
            if (isChecked) {
                showSetDecoyPinDialog();
            } else {
                controller.setDecoyEnabled(false);
            }
        });

        decoyChatsSubtitle = addClickableItem(container, "Soxta rejim chatlari", "", v -> openChatsSelector(true));
        updateDecoyChatsText();

        addInfoText(container, "Majburiy vaziyatda soxta parol terilsa, faqat soxta chatlar ko'rinadi va haqiqiy ma'lumotlar fosh bo'lmaydi.");

        // --- 3-BO'LIM: Xavfsizlik va Triggerlar ---
        addSectionHeader(container, "Xavfsizlik va Triggerlar");

        addClickableItem(container, "Asosiy parolni o'zgartirish", "Yangi master PIN o'rnatish", v -> showChangeMasterPinDialog());

        secretCodeSubtitle = addClickableItem(container, "Qidiruv maxfiy kodi", controller.getSecretCode(), v -> showEditSecretCodeDialog());

        addClickableItem(container, "Favqulodda tozalash (Panic PIN)", "Kiritilganda barcha stealth ma'lumotlarini tozalaydi", v -> showSetPanicPinDialog());

        autoLockCheckbox = addCheckboxItem(container, "Ilovadan chiqishda avto-qulflash", controller.isAutoLock(), (buttonView, isChecked) -> {
            controller.setAutoLock(isChecked);
        });

        strictNotifCheckbox = addCheckboxItem(container, "Yashirin chatlar bildirishnomasini bloklash", controller.isStrictNotifications(), (buttonView, isChecked) -> {
            controller.setStrictNotifications(isChecked);
        });

        addInfoText(container, "Qidiruv belgisini uzoq bosish yoki qidiruvga maxfiy kodni terish orqali yashirin menyuni ochish mumkin.");

        // --- 4-BO'LIM: Zaxira Nusxa (JSON) ---
        addSectionHeader(container, "Zaxira Nusxa (JSON)");

        addClickableItem(container, "Konfiguratsiyani JSON eksport qilish", "Ushbu sozlamalarni zaxira sifatida nusxalash", v -> exportJson());

        addClickableItem(container, "JSON dan qayta tiklash (Import)", "Eski konfiguratsiyani import qilish", v -> importJson());

        addInfoText(container, "Ilovani qayta o'rnatganda barcha sozlamalar va ko'rinadigan chatlar ro'yxatini tezda tiklash uchun JSON fayldan foydalaning.");

        return root;
    }

    private void updateAllowedChatsText() {
        if (allowedChatsSubtitle != null) {
            int count = StealthController.getInstance().getAllowedDialogIds().size();
            allowedChatsSubtitle.setText(count + " ta tanlangan");
        }
    }

    private void updateDecoyChatsText() {
        if (decoyChatsSubtitle != null) {
            int count = StealthController.getInstance().getDecoyDialogIds().size();
            decoyChatsSubtitle.setText(count + " ta tanlangan");
        }
    }

    private void openChatsSelector(boolean isDecoy) {
        StealthController controller = StealthController.getInstance();
        Set<Long> current = new LinkedHashSet<>(isDecoy ? controller.getDecoyDialogIds() : controller.getAllowedDialogIds());

        SelectChatsController c = new SelectChatsController();
        c.setArguments(new SelectChatsController.Arguments(
                SelectChatsController.MODE_SELECT_CHATS,
                (mode, chatIds, chatTypes) -> {
                    if (isDecoy) {
                        controller.setDecoyDialogIds(chatIds);
                        updateDecoyChatsText();
                    } else {
                        controller.setAllowedDialogIds(chatIds);
                        updateAllowedChatsText();
                    }
                },
                0, null, current, Collections.emptySet(), false
        ));
        navigateTo(c);
    }

    private void showChangeMasterPinDialog() {
        Context ctx = context();
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Asosiy Parolni O'zgartirish");

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = Screen.dp(20);
        layout.setPadding(pad, pad, pad, pad);

        EditText pinEdit = new EditText(ctx);
        pinEdit.setHint("Yangi parol");
        pinEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(pinEdit);

        EditText confirmEdit = new EditText(ctx);
        confirmEdit.setHint("Yangi parolni tasdiqlang");
        confirmEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(confirmEdit);

        builder.setView(layout);
        builder.setPositiveButton("Saqlash", (dialog, which) -> {
            String p1 = pinEdit.getText().toString().trim();
            String p2 = confirmEdit.getText().toString().trim();
            if (TextUtils.isEmpty(p1) || !p1.equals(p2)) {
                Toast.makeText(ctx, "Parollar mos kelmadi yoki bo'sh!", Toast.LENGTH_SHORT).show();
                return;
            }
            StealthController.getInstance().setMasterPin(p1);
            Toast.makeText(ctx, "Asosiy parol o'zgartirildi", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Bekor qilish", null);
        builder.show();
    }

    private void showSetDecoyPinDialog() {
        Context ctx = context();
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Soxta Parol O'rnatish");

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = Screen.dp(20);
        layout.setPadding(pad, pad, pad, pad);

        EditText pinEdit = new EditText(ctx);
        pinEdit.setHint("Soxta parol (PIN)");
        pinEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(pinEdit);

        builder.setView(layout);
        builder.setPositiveButton("Saqlash", (dialog, which) -> {
            String p = pinEdit.getText().toString().trim();
            if (TextUtils.isEmpty(p)) {
                if (decoyEnabledCheckbox != null) decoyEnabledCheckbox.setChecked(false);
                return;
            }
            StealthController.getInstance().setDecoyPin(p, true);
            if (decoyEnabledCheckbox != null) decoyEnabledCheckbox.setChecked(true);
            Toast.makeText(ctx, "Soxta parol saqlandi", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Bekor qilish", (dialog, which) -> {
            if (decoyEnabledCheckbox != null) decoyEnabledCheckbox.setChecked(false);
        });
        builder.show();
    }

    private void showSetPanicPinDialog() {
        Context ctx = context();
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Panic Parolini O'rnatish");

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = Screen.dp(20);
        layout.setPadding(pad, pad, pad, pad);

        TextView tv = new TextView(ctx);
        tv.setText("Ushbu PIN kiritilganda, barcha stealth sozlamalari darhol butunlay o'chiriladi.");
        tv.setTextSize(13);
        layout.addView(tv);

        EditText pinEdit = new EditText(ctx);
        pinEdit.setHint("Panic PIN");
        pinEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(pinEdit);

        builder.setView(layout);
        builder.setPositiveButton("Saqlash", (dialog, which) -> {
            String p = pinEdit.getText().toString().trim();
            if (!TextUtils.isEmpty(p)) {
                StealthController.getInstance().setPanicPin(p, true);
                Toast.makeText(ctx, "Panic PIN saqlandi", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Bekor qilish", null);
        builder.show();
    }

    private void showEditSecretCodeDialog() {
        Context ctx = context();
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Qidiruv Maxfiy Kodi");

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = Screen.dp(20);
        layout.setPadding(pad, pad, pad, pad);

        TextView tv = new TextView(ctx);
        tv.setText("Telegram qidiruv maydoniga shu kod yozilganda, ushbu yashirin menyu ochiladi.");
        tv.setTextSize(13);
        layout.addView(tv);

        EditText edit = new EditText(ctx);
        edit.setText(StealthController.getInstance().getSecretCode());
        layout.addView(edit);

        builder.setView(layout);
        builder.setPositiveButton("Saqlash", (dialog, which) -> {
            String val = edit.getText().toString().trim();
            if (!TextUtils.isEmpty(val)) {
                StealthController.getInstance().setSecretCode(val);
                if (secretCodeSubtitle != null) {
                    secretCodeSubtitle.setText(val);
                }
                Toast.makeText(ctx, "Maxfiy kod saqlandi: " + val, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Bekor qilish", null);
        builder.show();
    }

    private void exportJson() {
        Context ctx = context();
        String json = StealthController.getInstance().exportConfigJson();
        if (json == null) {
            Toast.makeText(ctx, "Eksport qilishda xatolik yuz berdi!", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Zaxira Nusxa (JSON)");

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = Screen.dp(20);
        layout.setPadding(pad, pad, pad, pad);

        EditText jsonEdit = new EditText(ctx);
        jsonEdit.setText(json);
        jsonEdit.setTextSize(12);
        jsonEdit.setMaxLines(8);
        layout.addView(jsonEdit);

        builder.setView(layout);
        builder.setPositiveButton("Ulashish (Share)", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, json);
            ctx.startActivity(Intent.createChooser(intent, "Stealth JSON konfiguratsiyasi"));
        });
        builder.setNegativeButton("Yopish", null);
        builder.show();
    }

    private void importJson() {
        Context ctx = context();
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Qayta Tiklash (Import)");

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = Screen.dp(20);
        layout.setPadding(pad, pad, pad, pad);

        TextView tv = new TextView(ctx);
        tv.setText("Eski zaxira JSON matnini shu yerga qo'ying:");
        layout.addView(tv);

        EditText jsonEdit = new EditText(ctx);
        jsonEdit.setHint("JSON matnini kiriting...");
        jsonEdit.setTextSize(12);
        jsonEdit.setMaxLines(8);
        layout.addView(jsonEdit);

        builder.setView(layout);
        builder.setPositiveButton("Tiklash", (dialog, which) -> {
            String jsonText = jsonEdit.getText().toString().trim();
            boolean success = StealthController.getInstance().importConfigJson(jsonText);
            if (success) {
                Toast.makeText(ctx, "Sozlamalar va chatlar muvaffaqiyatli tiklandi!", Toast.LENGTH_SHORT).show();
                updateAllowedChatsText();
                updateDecoyChatsText();
                if (secretCodeSubtitle != null) {
                    secretCodeSubtitle.setText(StealthController.getInstance().getSecretCode());
                }
                if (autoLockCheckbox != null) {
                    autoLockCheckbox.setChecked(StealthController.getInstance().isAutoLock());
                }
                if (strictNotifCheckbox != null) {
                    strictNotifCheckbox.setChecked(StealthController.getInstance().isStrictNotifications());
                }
            } else {
                Toast.makeText(ctx, "Xatolik: Noto'g'ri JSON formati!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Bekor qilish", null);
        builder.show();
    }

    private void addSectionHeader(LinearLayout container, String title) {
        TextView tv = new TextView(container.getContext());
        tv.setText(title);
        tv.setTextSize(15);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(0xFF3388EE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, Screen.dp(16), 0, Screen.dp(8));
        container.addView(tv, lp);
    }

    private TextView addClickableItem(LinearLayout container, String title, String subtitle, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(container.getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        int p = Screen.dp(12);
        row.setPadding(0, p, 0, p);
        row.setOnClickListener(listener);

        TextView tvTitle = new TextView(container.getContext());
        tvTitle.setText(title);
        tvTitle.setTextSize(16);
        tvTitle.setTextColor(0xFF222222);
        row.addView(tvTitle);

        TextView tvSub = new TextView(container.getContext());
        tvSub.setText(subtitle);
        tvSub.setTextSize(13);
        tvSub.setTextColor(0xFF888888);
        row.addView(tvSub);

        container.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return tvSub;
    }

    private CheckBox addCheckboxItem(LinearLayout container, String title, boolean initialValue, CheckBox.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(container.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int p = Screen.dp(12);
        row.setPadding(0, p, 0, p);

        TextView tvTitle = new TextView(container.getContext());
        tvTitle.setText(title);
        tvTitle.setTextSize(16);
        tvTitle.setTextColor(0xFF222222);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        row.addView(tvTitle, lp);

        CheckBox cb = new CheckBox(container.getContext());
        cb.setChecked(initialValue);
        cb.setOnCheckedChangeListener(listener);
        row.addView(cb);

        row.setOnClickListener(v -> cb.toggle());

        container.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return cb;
    }

    private void addInfoText(LinearLayout container, String text) {
        TextView tv = new TextView(container.getContext());
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTextColor(0xFF999999);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, Screen.dp(12));
        container.addView(tv, lp);
    }
}
