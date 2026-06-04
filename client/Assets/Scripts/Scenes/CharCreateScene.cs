using UnityEngine;

// Tao nhan vat theo CHUNG TOC + GIOI TINH + NGOAI HINH co ban (toc / mau da / quan ao).
// race: 0=Con Nguoi,1=Yeu Tinh. gender: 0=nam,1=nu. hair/skin/clothes = id appearance_options.
// Con nguoi & yeu tinh song hoa hop; nghe nghiep mo sau qua nhiem vu.
public class CharCreateScene : MonoBehaviour {
    int race = 0, gender = 0, hair = 0, skin = 0, clothes = 0;
    string charName = "";

    public void SetName(string n)   { charName = n != null ? n.Trim() : ""; }
    public void SetRace(int r)      { race = Mathf.Clamp(r, 0, 1); }
    public void SetGender(int g)    { gender = Mathf.Clamp(g, 0, 1); }
    public void SetHair(int h)      { hair = h; }
    public void SetSkin(int s)      { skin = s; }
    public void SetClothes(int c)   { clothes = c; }

    // Tuong thich nguoc: nut chon class cu (neu con) khong con tac dung — nghe nghiep chon sau.
    public void SetClass(int c)     { /* deprecated */ }

    public void Create() {
        if (charName.Length >= 2)
            PacketBuilder.SendCharCreate(charName, race, gender, hair, skin, clothes);
    }
}
