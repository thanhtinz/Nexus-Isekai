using UnityEngine;
using UnityEngine.EventSystems;
public class Joystick : MonoBehaviour, IDragHandler, IPointerUpHandler, IPointerDownHandler {
    [SerializeField] RectTransform bg, handle;
    public Vector2 Input { get; private set; }
    public void OnDrag(PointerEventData e) { Vector2 pos; RectTransformUtility.ScreenPointToLocalPointInRectangle(bg, e.position, e.pressEventCamera, out pos); pos /= bg.sizeDelta/2; Input = pos.magnitude > 1 ? pos.normalized : pos; handle.anchoredPosition = Input * bg.sizeDelta/2 * 0.4f; }
    public void OnPointerDown(PointerEventData e) { OnDrag(e); }
    public void OnPointerUp(PointerEventData e) { Input = Vector2.zero; handle.anchoredPosition = Vector2.zero; }
}