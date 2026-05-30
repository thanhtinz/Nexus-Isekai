package com.nexusisekai.ui.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.nexusisekai.R;
import com.nexusisekai.game.GameViewModel;
import com.nexusisekai.net.GameClient;
import com.nexusisekai.net.PacketWriter;
import com.nexusisekai.ui.view.GameSurfaceView;

public class GameActivity extends AppCompatActivity {

    private GameViewModel    vm;
    private GameSurfaceView  gameView;

    // UI overlays
    private TextView tvNotification;
    private LinearLayout chatBox;
    private ListView     lvChat;
    private EditText     etChatInput;
    private Button       btnChatSend;
    private Button       btnMenuInventory, btnMenuQuest, btnMenuSkill, btnMenuPet;
    private Button       btnGiftCode, btnWebshop;
    private ArrayAdapter<String> chatAdapter;

    private byte chatChannel = 1; // World

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Fullscreen landscape
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_game);

        vm = new ViewModelProvider(this).get(GameViewModel.class);

        // Game surface
        gameView = findViewById(R.id.gameSurface);
        gameView.setViewModel(vm);

        // UI refs
        tvNotification   = findViewById(R.id.tvNotification);
        chatBox          = findViewById(R.id.chatBox);
        lvChat           = findViewById(R.id.lvChat);
        etChatInput      = findViewById(R.id.etChatInput);
        btnChatSend      = findViewById(R.id.btnChatSend);
        btnMenuInventory = findViewById(R.id.btnInventory);
        btnMenuQuest     = findViewById(R.id.btnQuest);
        btnMenuSkill     = findViewById(R.id.btnSkill);
        btnMenuPet       = findViewById(R.id.btnPet);
        btnGiftCode      = findViewById(R.id.btnGiftCode);

        chatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        lvChat.setAdapter(chatAdapter);

        // Observe
        vm.notification.observe(this, msg -> {
            if (msg != null) {
                tvNotification.setText(msg);
                tvNotification.setVisibility(View.VISIBLE);
                tvNotification.postDelayed(() -> tvNotification.setVisibility(View.GONE), 3000);
                gameView.showNotification(msg, 2500);
            }
        });

        vm.chatHistory.observe(this, msgs -> {
            if (msgs == null) return;
            chatAdapter.clear();
            for (GameViewModel.ChatMessage m : msgs)
                chatAdapter.add("[" + m.channel + "] " + m.sender + ": " + m.content);
            lvChat.setSelection(chatAdapter.getCount() - 1);
        });

        vm.disconnected.observe(this, ok -> {
            if (Boolean.TRUE.equals(ok)) {
                new AlertDialog.Builder(this)
                    .setTitle("Mất kết nối")
                    .setMessage("Kết nối server bị gián đoạn.")
                    .setPositiveButton("Đăng nhập lại", (d,w) -> finish())
                    .setCancelable(false).show();
            }
        });

        vm.mapName.observe(this, name -> {
            if (name != null) gameView.showNotification("→ " + name, 1500);
        });

        // Buttons
        btnChatSend.setOnClickListener(v -> sendChat());
        etChatInput.setOnEditorActionListener((v, actionId, event) -> { sendChat(); return true; });

        btnMenuInventory.setOnClickListener(v -> {
            GameClient.getInstance().send(PacketWriter.inventoryList());
            showInventoryDialog();
        });

        btnMenuQuest.setOnClickListener(v -> {
            GameClient.getInstance().send(PacketWriter.questList());
            showQuestDialog();
        });

        btnMenuSkill.setOnClickListener(v -> {
            GameClient.getInstance().send(PacketWriter.skillList());
            showSkillDialog();
        });

        btnMenuPet.setOnClickListener(v -> {
            GameClient.getInstance().send(PacketWriter.petList());
        });

        btnGiftCode.setOnClickListener(v -> showGiftCodeDialog());

        // Channel tabs (tap header to cycle)
        tvNotification.setOnClickListener(v -> {
            chatChannel = (byte)((chatChannel + 1) % 6);
            String label = switch (chatChannel) { case 0->"Map"; case 1->"World"; case 2->"Guild"; case 3->"PM"; case 5->"Cross"; default->"System"; };
            Toast.makeText(this, "Kênh: " + label, Toast.LENGTH_SHORT).show();
        });

        // Auto-ping
        gameView.postDelayed(new Runnable() {
            public void run() {
                GameClient.getInstance().send(PacketWriter.ping());
                gameView.postDelayed(this, 30_000);
            }
        }, 30_000);
    }

    private void sendChat() {
        String msg = etChatInput.getText().toString().trim();
        if (msg.isEmpty()) return;
        GameClient.getInstance().send(PacketWriter.chat(chatChannel, msg));
        etChatInput.setText("");
    }

    private void showInventoryDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this).setTitle("Túi Đồ").setNegativeButton("Đóng", null);
        vm.inventory.observe(this, items -> {
            if (items == null || items.isEmpty()) { b.setMessage("Túi đồ trống."); return; }
            String[] names = new String[items.size()];
            for (int i=0;i<items.size();i++) {
                GameViewModel.InventoryItem it = items.get(i);
                names[i] = it.name + (it.enhanceLevel>0?" +"+it.enhanceLevel:"") + " x" + it.qty;
            }
            b.setItems(names, (d, which) -> {
                GameViewModel.InventoryItem selected = items.get(which);
                new AlertDialog.Builder(this)
                    .setTitle(selected.name)
                    .setItems(new String[]{"Dùng","Trang bị","Bỏ"}, (dd, w) -> {
                        switch(w) {
                            case 0 -> GameClient.getInstance().send(PacketWriter.useItem(selected.instanceId));
                            case 1 -> GameClient.getInstance().send(PacketWriter.equipItem(selected.instanceId));
                            case 2 -> GameClient.getInstance().send(PacketWriter.dropItem(selected.instanceId));
                        }
                    }).setNegativeButton("Huỷ",null).show();
            });
            b.show();
        });
    }

    private void showQuestDialog() {
        vm.quests.observe(this, quests -> {
            if (quests == null || quests.isEmpty()) {
                Toast.makeText(this, "Chưa có nhiệm vụ nào.", Toast.LENGTH_SHORT).show(); return;
            }
            String[] names = new String[quests.size()];
            for (int i=0;i<quests.size();i++) {
                GameViewModel.QuestData q = quests.get(i);
                names[i] = (q.completed ? "[V] " : "") + q.title + " " + q.progress + "/" + q.target;
            }
            new AlertDialog.Builder(this).setTitle("Nhiệm Vụ")
                .setItems(names, (d, which) -> {
                    GameViewModel.QuestData q = quests.get(which);
                    if (q.completed)
                        GameClient.getInstance().send(PacketWriter.questComplete(q.id));
                })
                .setNegativeButton("Đóng", null).show();
        });
    }

    private void showSkillDialog() {
        vm.skills.observe(this, skills -> {
            if (skills == null || skills.isEmpty()) {
                Toast.makeText(this, "Chưa có kỹ năng nào.", Toast.LENGTH_SHORT).show(); return;
            }
            String[] names = new String[skills.size()];
            for (int i=0;i<skills.size();i++) {
                GameViewModel.SkillData s = skills.get(i);
                names[i] = s.name + " Lv." + s.level + " MP:" + s.mpCost;
            }
            new AlertDialog.Builder(this).setTitle("Kỹ Năng")
                .setItems(names, (d, which) -> {
                    int skillId = skills.get(which).id;
                    new AlertDialog.Builder(this)
                        .setTitle(skills.get(which).name)
                        .setItems(new String[]{"Dùng","Đặt slot 1","Đặt slot 2","Nâng cấp"}, (dd,w) -> {
                            switch(w){
                                case 0 -> GameClient.getInstance().send(PacketWriter.useSkill(skillId, 0));
                                case 1 -> GameClient.getInstance().send(PacketWriter.skillSetSlot(0, skillId));
                                case 2 -> GameClient.getInstance().send(PacketWriter.skillSetSlot(1, skillId));
                                case 3 -> GameClient.getInstance().send(PacketWriter.skillUpgrade(skillId));
                            }
                        }).setNegativeButton("Huỷ",null).show();
                })
                .setNegativeButton("Đóng",null).show();
        });
    }

    private void showGiftCodeDialog() {
        EditText et = new EditText(this); et.setHint("Nhập gift code...");
        new AlertDialog.Builder(this).setTitle("Gift Code").setView(et)
            .setPositiveButton("Dùng", (d,w) -> {
                String code = et.getText().toString().trim().toUpperCase();
                if (!code.isEmpty()) GameClient.getInstance().send(PacketWriter.giftCode(code));
            }).setNegativeButton("Huỷ",null).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GameClient.getInstance().disconnect();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
            .setTitle("Thoát game?")
            .setPositiveButton("Thoát", (d,w) -> { GameClient.getInstance().disconnect(); finish(); })
            .setNegativeButton("Huỷ", null).show();
    }
}
