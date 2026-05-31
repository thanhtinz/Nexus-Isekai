using UnityEngine;
public class MiniMap : MonoBehaviour {
    [SerializeField] Camera miniMapCam;
    [SerializeField] Transform player;
    void LateUpdate() { if (player && miniMapCam) { var p = player.position; miniMapCam.transform.position = new Vector3(p.x, p.y+10, p.z); } }
}