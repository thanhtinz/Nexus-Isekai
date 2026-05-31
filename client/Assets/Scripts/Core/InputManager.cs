using UnityEngine;
public class InputManager : MonoBehaviour {
    public static InputManager Instance;
    public Vector2 MoveInput { get; private set; }
    public bool AttackPressed { get; private set; }
    void Awake() { if (!Instance) Instance=this; else Destroy(gameObject); }
    void Update() {
        MoveInput = new Vector2(Input.GetAxis("Horizontal"), Input.GetAxis("Vertical"));
        AttackPressed = Input.GetButtonDown("Fire1");
    }
}