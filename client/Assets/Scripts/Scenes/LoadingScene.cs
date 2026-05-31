using UnityEngine;
using UnityEngine.SceneManagement;
public class LoadingScene : MonoBehaviour {
    float timer;
    void Start() { timer = 0; }
    void Update() { timer += Time.deltaTime; if (timer >= 2f) SceneManager.LoadScene("Login"); }
}