package jjdm.zocalo.rules;

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.CouponBank;
import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.experiment.role.AbstractSubject;
import net.commerce.zocalo.experiment.role.Role;
import net.commerce.zocalo.market.BinaryMarket;

import java.util.Map;
import java.util.Properties;

/**
 * Used to extract the current session data/state and pass it. Created by Josh Martin on 9/11/2015.
 */
public class SessionData {

	private Session session;
	private Properties properties;
	private CashBank cashBank;
	private CouponBank couponBank;
	private BinaryMarket market;
	private Map<String, Role> roles;
	private BinaryClaim claim;
	private Map<String, AbstractSubject> players;
	private int currentRound;
	private CashBank rootBank;

	public SessionData(Session session) {
		super();
		this.session = session;
	}

	// these may need to be passed
//	private Price average;
//	private Quantity judged;
//	private Quantity judgingTarget;
//	private String outcome;

	public CashBank getCashBank() {
		return cashBank;
	}

	public void setCashBank(CashBank cashBank) {
		this.cashBank = cashBank;
	}

	public BinaryClaim getClaim() {
		return claim;
	}

	public void setClaim(BinaryClaim claim) {
		this.claim = claim;
	}

	public CouponBank getCouponBank() {
		return couponBank;
	}

	public void setCouponBank(CouponBank couponBank) {
		this.couponBank = couponBank;
	}

	public int getCurrentRound() {
		return currentRound;
	}

	public void setCurrentRound(int currentRound) {
		this.currentRound = currentRound;
	}

	public BinaryMarket getMarket() {
		return market;
	}

	public void setMarket(BinaryMarket market) {
		this.market = market;
	}

	public Map<String, AbstractSubject> getPlayers() {
		return players;
	}

	public void setPlayers(Map<String, AbstractSubject> players) {
		this.players = players;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public Map<String, Role> getRoles() {
		return roles;
	}

	public void setRoles(Map<String, Role> roles) {
		this.roles = roles;
	}

	public CashBank getRootBank() {
		return rootBank;
	}

	public void setRootBank(CashBank rootBank) {
		this.rootBank = rootBank;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}
}
