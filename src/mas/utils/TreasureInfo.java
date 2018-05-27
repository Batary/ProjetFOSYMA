package mas.utils;

import java.io.Serializable;

import env.Attribute;

public class TreasureInfo  implements Serializable {

	private static final long serialVersionUID = 7846848645754140261L;

	public TreasureInfo(Attribute type, int amount, String position) {
		this.type = type;
		this.amount = amount;
		this.position = position;

		this.lastUpdate = System.currentTimeMillis();
	}

	// possible attributes (bool) : collected , lost ?

	public Attribute type;
	public int amount;
	public String position;
	public String collectorAgent;

	public long lastUpdate = 0;

	public void update(int amount) {
		this.amount = amount;
		this.lastUpdate = System.currentTimeMillis();
	}

	public void assign(String agent) {
		this.collectorAgent = agent;
		this.lastUpdate = System.currentTimeMillis();
	}

	public void merge(TreasureInfo t) {
		if (t.lastUpdate > lastUpdate) {
			lastUpdate = t.lastUpdate;
		}

		// this block does not handle agent swap, but it should not be possible at the moment
		if (t.collectorAgent != null && collectorAgent == null) {
			collectorAgent = t.collectorAgent;
		}

		if (t.amount < amount) {
			amount = t.amount;
		}

	}

}
