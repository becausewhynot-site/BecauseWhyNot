import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ShopHandler {
	public static final int MAX_SHOPS = 101;
	public static final int MAX_SHOP_ITEMS = 101;
	public static final int MAX_IN_SHOP_ITEMS = 40;
	public static final int MAX_SHOW_DELAY = 60;

	public static int totalShops = 0;
	public static int[][] shopItems = new int[MAX_SHOPS][MAX_SHOP_ITEMS];
	public static int[][] shopItemsQuantity = new int[MAX_SHOPS][MAX_SHOP_ITEMS];
	public static int[][] shopItemsDelay = new int[MAX_SHOPS][MAX_SHOP_ITEMS];
	public static int[][] shopItemsSpecialNumbers = new int[MAX_SHOPS][MAX_SHOP_ITEMS];
	public static int[] shopItemsStandard = new int[MAX_SHOPS];
	public static String[] shopNames = new String[MAX_SHOPS];
	public static int[] shopSellModifier = new int[MAX_SHOPS];
	public static int[] shopBuyModifier = new int[MAX_SHOPS];

	public ShopHandler() {
		initializeShops();
		totalShops = 0;
		loadShops("shops.cfg");
	}

	public void process() {
		boolean didUpdate = false;
		int maxShopItems = MAX_SHOP_ITEMS;

		for (int i = 1; i <= totalShops; i++) {
			for (int j = 0; j < maxShopItems; j++) {
				int currentQuantity = shopItemsQuantity[i][j];
				int specialNumber = shopItemsSpecialNumbers[i][j];

				if (shopItems[i][j] > 0) {
					if (shopItemsDelay[i][j] >= MAX_SHOW_DELAY) {
						if (j <= shopItemsStandard[i] && currentQuantity <= specialNumber) {
							if (currentQuantity < specialNumber) {
								shopItemsQuantity[i][j]++;
							}
						} else {
							discountItem(i, j);
						}
						shopItemsDelay[i][j] = 0;
						didUpdate = true;
					}
					shopItemsDelay[i][j]++;
				}
			}
			if (didUpdate) {
				updatePlayers(i);
				didUpdate = false;
			}
		}
	}

	public void discountItem(int shopID, int arrayID) {
		shopItemsQuantity[shopID][arrayID]--;
		int currentQuantity = shopItemsQuantity[shopID][arrayID];

		if (currentQuantity <= 0) {
			shopItemsQuantity[shopID][arrayID] = 0;
			resetItem(shopID, arrayID);
		}
	}

	public void resetItem(int shopID, int arrayID) {
		shopItems[shopID][arrayID] = 0;
		shopItemsQuantity[shopID][arrayID] = 0;
		shopItemsDelay[shopID][arrayID] = 0;
	}

	private void initializeShops() {
		int maxShopItems = MAX_SHOP_ITEMS;

		for (int i = 0; i < MAX_SHOPS; i++) {
			for (int j = 0; j < maxShopItems; j++) {
				resetItem(i, j);
				shopItemsSpecialNumbers[i][j] = 0;
			}
			shopItemsStandard[i] = 0;
			shopSellModifier[i] = 0;
			shopBuyModifier[i] = 0;
			shopNames[i] = "";
		}
	}

	private void updatePlayers(int shopID) {
		int maxPlayers = Server.playerHandler.maxPlayers;

		for (int k = 1; k < maxPlayers; k++) {
			if (Server.playerHandler.players[k] != null) {
				if (Server.playerHandler.players[k].isShopping && Server.playerHandler.players[k].MyShopID == shopID) {
					Server.playerHandler.players[k].UpdateShop = true;
				}
			}
		}
	}

	private boolean loadShops(String filename) {
		String line = "";
		String token = "";
		String token2 = "";
		String token2_2 = "";
		String[] token3 = new String[(MAX_SHOP_ITEMS * 2)];
		boolean endOfFile = false;
		BufferedReader characterfile = null;

		try {
			characterfile = new BufferedReader(new FileReader("./assets/config/" + filename));
		} catch (FileNotFoundException fileex) {
			Misc.println(filename + ": file not found.");
			return false;
		}

		try {
			line = characterfile.readLine();
		} catch (IOException ioexception) {
			Misc.println(filename + ": error loading file.");
			try {
				characterfile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}

		while (!endOfFile && line != null) {
			line = line.trim();
			int spot = line.indexOf("=");

			if (spot > -1) {
				token = line.substring(0, spot);
				token = token.trim();
				token2 = line.substring(spot + 1);
				token2 = token2.trim();
				token2_2 = token2.replaceAll("\t\t", "\t").replaceAll("\t\t", "\t").replaceAll("\t\t", "\t")
						.replaceAll("\t\t", "\t").replaceAll("\t\t", "\t");
				token3 = token2_2.split("\t");

				if (token.equals("shop")) {
					int shopID = Integer.parseInt(token3[0]);
					shopNames[shopID] = token3[1].replaceAll("_", " ");
					shopSellModifier[shopID] = Integer.parseInt(token3[2]);
					shopBuyModifier[shopID] = Integer.parseInt(token3[3]);

					for (int i = 0; i < ((token3.length - 4) / 2); i++) {
						if (token3[(4 + (i * 2))] != null) {
							shopItems[shopID][i] = (Integer.parseInt(token3[(4 + (i * 2))]) + 1);
							shopItemsQuantity[shopID][i] = Integer.parseInt(token3[(5 + (i * 2))]);
							shopItemsSpecialNumbers[shopID][i] = Integer.parseInt(token3[(5 + (i * 2))]);
							shopItemsStandard[shopID]++;
						} else {
							break;
						}
					}
					totalShops++;
				}
			} else {
				if (line.equals("[ENDOFSHOPLIST]")) {
					try {
						characterfile.close();
					} catch (IOException ioexception) {
						ioexception.printStackTrace();
					}
					return true;
				}
			}

			try {
				line = characterfile.readLine();
			} catch (IOException ioexception1) {
				endOfFile = true;
			}
		}

		try {
			characterfile.close();
		} catch (IOException ioexception) {
			ioexception.printStackTrace();
		}
		return false;
	}
}