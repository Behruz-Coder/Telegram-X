package org.thunderdog.challegram.stealth;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.tool.Screen;

public class StealthDialogHelper {

    public static void showUnlockOrSetupDialog(ViewController<?> controller) {
        if (controller == null || controller.context() == null) {
            return;
        }

        StealthController stealthController = StealthController.getInstance();
        if (!stealthController.isPasswordSet()) {
            showSetupDialog(controller);
        } else {
            showUnlockDialog(controller);
        }
    }

    public static void showSetupDialog(ViewController<?> controller) {
        Context context = controller.context();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Yashirin Menyu: Parol O'rnatish");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = Screen.dp(20);
        layout.setPadding(pad, pad, pad, pad);

        TextView messageText = new TextView(context);
        messageText.setText("Yashirin menyuga kirish va ko'rinadigan chatlarni boshqarish uchun yangi parol o'rnating.");
        messageText.setTextSize(14);
        layout.addView(messageText);

        EditText pinEditText = new EditText(context);
        pinEditText.setHint("Yangi parol");
        pinEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pinEditText.setTextSize(16);
        layout.addView(pinEditText);

        EditText confirmEditText = new EditText(context);
        confirmEditText.setHint("Parolni tasdiqlang");
        confirmEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmEditText.setTextSize(16);
        layout.addView(confirmEditText);

        builder.setView(layout);

        builder.setPositiveButton("Saqlash", (dialog, which) -> {
            String pin = pinEditText.getText().toString().trim();
            String confirmPin = confirmEditText.getText().toString().trim();

            if (TextUtils.isEmpty(pin)) {
                Toast.makeText(context, "Parol bo'sh bo'lishi mumkin emas!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!pin.equals(confirmPin)) {
                Toast.makeText(context, "Parollar mos kelmadi!", Toast.LENGTH_SHORT).show();
                return;
            }

            StealthController.getInstance().setMasterPin(pin);
            Toast.makeText(context, "Parol muvaffaqiyatli saqlandi!", Toast.LENGTH_SHORT).show();
            controller.navigateTo(new StealthManageController());
        });

        builder.setNegativeButton("Bekor qilish", null);
        builder.show();
    }

    public static void showUnlockDialog(ViewController<?> controller) {
        Context context = controller.context();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Xavfsizlik Paroli");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = Screen.dp(20);
        layout.setPadding(pad, pad, pad, pad);

        TextView messageText = new TextView(context);
        messageText.setText("Yashirin menyuga kirish uchun parolni kiriting:");
        messageText.setTextSize(14);
        layout.addView(messageText);

        EditText pinEditText = new EditText(context);
        pinEditText.setHint("Parol");
        pinEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pinEditText.setTextSize(16);
        layout.addView(pinEditText);

        builder.setView(layout);

        builder.setPositiveButton("Kirish", (dialog, which) -> {
            String pin = pinEditText.getText().toString().trim();
            int result = StealthController.getInstance().verifyPin(pin);

            if (result == StealthController.RESULT_MASTER) {
                controller.navigateTo(new StealthManageController());
            } else if (result == StealthController.RESULT_DECOY) {
                Toast.makeText(context, "Soxta rejim faollashtirildi", Toast.LENGTH_SHORT).show();
            } else if (result == StealthController.RESULT_PANIC) {
                Toast.makeText(context, "Xavfsizlik sozlamalari tozalandi", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Noto'g'ri parol!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Bekor qilish", null);
        builder.show();
    }
}
