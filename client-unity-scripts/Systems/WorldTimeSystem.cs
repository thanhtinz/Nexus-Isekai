using UnityEngine;
using UnityEngine.Rendering.Universal;
using FantasyRealm.Network;

namespace FantasyRealm.Systems
{
    public class WorldTimeSystem : MonoBehaviour
    {
        public static WorldTimeSystem Instance { get; private set; }

        [Header("Lighting")]
        public Light2D globalLight;
        public Gradient timeOfDayGradient;

        [Header("Season Particles")]
        public ParticleSystem springParticles;
        public ParticleSystem summerParticles;
        public ParticleSystem autumnParticles;
        public ParticleSystem winterParticles;

        [Header("State")]
        public int  GameHour;
        public int  GameMinute;
        public int  TimeOfDay;  // 0=DAWN .. 5=NIGHT
        public int  Season;     // 0=SPRING..3=WINTER
        public bool IsFullMoon;

        public static readonly string[] TimeNames   = {"Bình Minh","Ban Ngày","Buổi Trưa","Buổi Chiều","Hoàng Hôn","Ban Đêm"};
        public static readonly string[] SeasonNames = {"Xuân","Hạ","Thu","Đông"};

        void Awake() { if (Instance != null) { Destroy(gameObject); return; } Instance = this; }

        void Start() {
            PacketRouter.Instance.Register(PacketType.S_TIME_UPDATE, OnTimeUpdate);
        }

        void OnTimeUpdate(Packet p) {
            GameHour   = p.ReadByte();
            GameMinute = p.ReadByte();
            TimeOfDay  = p.ReadByte();
            int newSeason = p.ReadByte();
            IsFullMoon = p.ReadBool();
            bool seasonChanged = newSeason != Season;
            Season = newSeason;
            UpdateLighting();
            if (seasonChanged) UpdateSeasonParticles();
        }

        void UpdateLighting() {
            if (globalLight == null || timeOfDayGradient == null) return;
            float t = GameHour / 24f + GameMinute / 1440f;
            globalLight.color     = timeOfDayGradient.Evaluate(t);
            globalLight.intensity = TimeOfDay == 5 ? 0.3f : TimeOfDay == 0 || TimeOfDay == 4 ? 0.7f : 1f;
            if (IsFullMoon && TimeOfDay == 5) { globalLight.intensity = 0.6f; globalLight.color = Color.cyan * 0.5f + Color.white * 0.5f; }
        }

        void UpdateSeasonParticles() {
            var systems = new[] { springParticles, summerParticles, autumnParticles, winterParticles };
            for (int i = 0; i < systems.Length; i++) {
                if (systems[i] == null) continue;
                if (i == Season) { if (!systems[i].isPlaying) systems[i].Play(); }
                else             { if (systems[i].isPlaying)  systems[i].Stop(); }
            }
        }
    }
}
