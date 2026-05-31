using UnityEngine;
/// <summary>FarmUI — nông trại: trồng/tưới/bón phân/thu hoạch, nuôi/sinh sản thú, thăm vườn.</summary>
public class FarmUI : MonoBehaviour {
    public static FarmUI Instance;
    void Awake(){ if(!Instance) Instance=this; else Destroy(gameObject); }
    public void Plant(int plot, int seedItemId) => PacketBuilder.SendFarmPlant(plot, seedItemId);
    public void Water(int plot) => PacketBuilder.SendFarmWater(plot);
    public void Fertilize(int plotIndex) => PacketBuilder.SendFarmFertilize(plotIndex);
    public void Harvest(int plot) => PacketBuilder.SendFarmHarvest(plot);
    public void Breed(int penIndex) => PacketBuilder.SendAnimalBreed(penIndex);
    public void Visit(long ownerCharId) => PacketBuilder.SendFarmVisit(ownerCharId);
    public void BeginVisit(long owner){ }
    public void AddVisitPlot(int plot, int seed, int stage){ }
    public void RefreshAnimals(){ }
}
