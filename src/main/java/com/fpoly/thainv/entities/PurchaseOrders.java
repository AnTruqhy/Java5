package com.fpoly.thainv.entities;
// Generated Jun 6, 2024, 12:44:43 AM by Hibernate Tools 4.3.6.Final

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * PurchaseOrders generated by hbm2java
 */
@Entity
@Table(name = "purchase_orders", schema = "dbo", catalog = "TMDT")
public class PurchaseOrders implements java.io.Serializable {

	private Integer purchaseOrderId;
	private OrderStatus orderStatus;
	private Suppliers suppliers;
	private Date orderDate;
	private Date deliveryDate;
	private Set<PurchaseOrderDetails> purchaseOrderDetailses = new HashSet<PurchaseOrderDetails>(0);

	public PurchaseOrders() {
	}

	public PurchaseOrders(Date orderDate) {
		this.orderDate = orderDate;
	}

	public PurchaseOrders(OrderStatus orderStatus, Suppliers suppliers, Date orderDate, Date deliveryDate,
			Set<PurchaseOrderDetails> purchaseOrderDetailses) {
		this.orderStatus = orderStatus;
		this.suppliers = suppliers;
		this.orderDate = orderDate;
		this.deliveryDate = deliveryDate;
		this.purchaseOrderDetailses = purchaseOrderDetailses;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)

	@Column(name = "purchase_order_id", unique = true, nullable = false)
	public Integer getPurchaseOrderId() {
		return this.purchaseOrderId;
	}

	public void setPurchaseOrderId(Integer purchaseOrderId) {
		this.purchaseOrderId = purchaseOrderId;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "status_id")
	public OrderStatus getOrderStatus() {
		return this.orderStatus;
	}

	public void setOrderStatus(OrderStatus orderStatus) {
		this.orderStatus = orderStatus;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id")
	public Suppliers getSuppliers() {
		return this.suppliers;
	}

	public void setSuppliers(Suppliers suppliers) {
		this.suppliers = suppliers;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "order_date", nullable = false, length = 10)
	public Date getOrderDate() {
		return this.orderDate;
	}

	public void setOrderDate(Date orderDate) {
		this.orderDate = orderDate;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "delivery_date", length = 10)
	public Date getDeliveryDate() {
		return this.deliveryDate;
	}

	public void setDeliveryDate(Date deliveryDate) {
		this.deliveryDate = deliveryDate;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "purchaseOrders")
	public Set<PurchaseOrderDetails> getPurchaseOrderDetailses() {
		return this.purchaseOrderDetailses;
	}

	public void setPurchaseOrderDetailses(Set<PurchaseOrderDetails> purchaseOrderDetailses) {
		this.purchaseOrderDetailses = purchaseOrderDetailses;
	}

}