using UnityEngine;
public class CharCreateScene : MonoBehaviour {
    int classId = 1, gender = 0;
    string charName = "";
    public void SetClass(int c) { classId = c; }
    public void SetGender(int g) { gender = g; }
    public void Create() { if (charName.Length >= 2) PacketBuilder.SendCharCreate(charName, classId, gender); }
}