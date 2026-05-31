using UnityEngine;
public class FloatingText : MonoBehaviour {
    float speed = 1f, lifetime = 1.5f, timer;
    void Update() { timer+=Time.deltaTime; transform.Translate(Vector3.up*speed*Time.deltaTime); if (timer>=lifetime) Destroy(gameObject); }
    public static void Spawn(Vector3 pos, string text, Color color) {
        // Instantiate floating text prefab at pos
    }
}