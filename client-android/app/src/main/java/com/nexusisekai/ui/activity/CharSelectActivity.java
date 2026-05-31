package com.nexusisekai.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.nexusisekai.R;
import com.nexusisekai.game.GameViewModel;
import com.nexusisekai.net.GameClient;
import com.nexusisekai.net.PacketWriter;
import java.util.List;

// ════════════════════════════════════════════════════════
// CharSelectActivity
// ════════════════════════════════════════════════════════

public class CharSelectActivity extends AppCompatActivity {

    private GameViewModel vm;
    private ListView      lvChars;
    private Button        btnNewChar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_char_select);

        vm       = new ViewModelProvider(this).get(GameViewModel.class);
        lvChars  = findViewById(R.id.lvCharacters);
        btnNewChar = findViewById(R.id.btnNewChar);

        vm.charSlots.observe(this, this::bindCharList);
        vm.enterGame.observe(this, ok -> {
            if (Boolean.TRUE.equals(ok)) {
                startActivity(new Intent(this, GameActivity.class));
                finish();
            }
        });
        vm.notification.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        btnNewChar.setOnClickListener(v -> showCreateDialog());

        // Refresh list
        GameClient.getInstance().send(PacketWriter.charList());
    }

    private void bindCharList(List<GameViewModel.CharSlot> slots) {
        if (slots == null) return;
        String[] items = new String[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            GameViewModel.CharSlot s = slots.get(i);
            items[i] = s.name + "  Lv." + s.level + "  [" + s.className + "]";
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        lvChars.setAdapter(adapter);

        lvChars.setOnItemClickListener((parent, view, pos, id) -> {
            if (pos < slots.size())
                GameClient.getInstance().send(PacketWriter.charSelect(slots.get(pos).charId));
        });

        lvChars.setOnItemLongClickListener((parent, view, pos, id) -> {
            if (pos < slots.size()) {
                new AlertDialog.Builder(this)
                    .setTitle("Xoá nhân vật?")
                    .setMessage(slots.get(pos).name)
                    .setPositiveButton("Xoá", (d, w) -> {
                        GameClient.getInstance().send(new PacketWriter((short)0x0203).writeLong(slots.get(pos).charId));
                        GameClient.getInstance().send(PacketWriter.charList());
                    })
                    .setNegativeButton("Huỷ", null)
                    .show();
            }
            return true;
        });
    }

    private void showCreateDialog() {
        View v      = LayoutInflater.from(this).inflate(R.layout.dialog_create_char, null);
        EditText etName = v.findViewById(R.id.etCharName);
        Spinner  spClass = v.findViewById(R.id.spClass);
        RadioGroup rgGender = v.findViewById(R.id.rgGender);

        String[] classes = {"Kiếm Sĩ","Sát Thủ","Pháp Sư","Pháp Thủ","Cung Thủ"};
        spClass.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, classes));

        new AlertDialog.Builder(this)
            .setTitle("Tạo nhân vật")
            .setView(v)
            .setPositiveButton("Tạo", (d, w) -> {
                String name = etName.getText().toString().trim();
                if (name.length() < 2) { Toast.makeText(this,"Tên ≥ 2 ký tự", Toast.LENGTH_SHORT).show(); return; }
                // Appearance — class chọn sau tại NPC
                int bodyType   = spBody != null ? spBody.getSelectedItemPosition() + 1 : 1;
                int skinColor  = sbSkin != null ? sbSkin.getProgress() : 1;
                int eyeStyle   = sbEye  != null ? sbEye.getProgress()  : 0;
                int hairStyle  = spHair != null ? spHair.getSelectedItemPosition() : 0;
                int hairColor  = sbHairColor != null ? sbHairColor.getProgress() : 1;
                int shirtColor = sbShirt != null ? sbShirt.getProgress() + 1 : 1;
                int pantsColor = sbPants != null ? sbPants.getProgress() + 1 : 1;
                GameClient.getInstance().send(PacketWriter.charCreate(name,
                    bodyType, skinColor, eyeStyle, hairStyle, hairColor, shirtColor, pantsColor));
            })
            .setNegativeButton("Huỷ", null)
            .show();
    }
}
