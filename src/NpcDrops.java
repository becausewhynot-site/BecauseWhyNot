public class NpcDrops {
	public int npcId;
	public int DropType;
	public int[] Items = new int[100];
	public int[] ItemsN = new int[100];

	public NpcDrops(int _npcId) {
		npcId = _npcId;
		for (int i = 0; i < Items.length; i++) {
			Items[i] = -1;
			ItemsN[i] = 0;
		}
	}
}
