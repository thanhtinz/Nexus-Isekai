using UnityEngine;
public class MovementController : MonoBehaviour {
    public float moveSpeed = 5f;
    void Update() {
        var input = InputManager.Instance?.MoveInput ?? Vector2.zero;
        if (input.sqrMagnitude > 0.01f) {
            transform.Translate(input * moveSpeed * Time.deltaTime);
            PacketBuilder.SendPosition(transform.position.x, transform.position.y);
        }
    }
}