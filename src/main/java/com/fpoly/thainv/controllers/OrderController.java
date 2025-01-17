package com.fpoly.thainv.controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fpoly.thainv.entities.Addresses;
import com.fpoly.thainv.entities.AttributeProduct;
import com.fpoly.thainv.entities.Attributes;
import com.fpoly.thainv.entities.Coupons;
import com.fpoly.thainv.entities.OrderDetails;
import com.fpoly.thainv.entities.OrderStatus;
import com.fpoly.thainv.entities.Orders;
import com.fpoly.thainv.jpa.AddressJpa;
import com.fpoly.thainv.jpa.AttributeProductJPA;
import com.fpoly.thainv.jpa.OrderDetailJpa;
import com.fpoly.thainv.jpa.OrderJpa;
import com.fpoly.thainv.jpa.OrderStatusJpa;
import com.fpoly.thainv.jpa.ProductJPA;
import com.fpoly.thainv.services.AttributeProductService;
import com.fpoly.thainv.services.AttributesService;
import com.fpoly.thainv.services.OrderService;

@Controller
public class OrderController {

	@Autowired
	OrderJpa orderJpa;

	@Autowired
	OrderDetailJpa orderDetailJpa;

	@Autowired
	ProductJPA productJpa;

	@Autowired
	OrderStatusJpa orderStatusJpa;

	@Autowired
	AddressJpa addressJpa;

	private final OrderService orderService;

	@Autowired
	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@Autowired
	private AttributesService attributesService;

	@Autowired
	private AttributeProductJPA attributeProductJpa;

	public List<AttributeProduct> findByProductIdAndAttributeIds(int productId, int colorId, int sizeId) {
		return attributeProductJpa.findByProductIdAndAttributeIds(productId, colorId, sizeId);
	}

	public void save(AttributeProduct attributeProduct) {
		attributeProductJpa.save(attributeProduct);
	}

	@Autowired
	private AttributeProductService attributeProductService;

	@ModelAttribute("statuses")
	public List<OrderStatus> getOrderStatus() {
		return orderStatusJpa.findAll();
	}

	@ModelAttribute("addresses")
	public List<Addresses> getAddress() {
		return addressJpa.findAll();
	}

	@ModelAttribute("orders")
	public Page<Orders> getOrders(@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "address", required = false) String address,
			@RequestParam(value = "status", required = false) String status, Pageable pageable) {

		return orderJpa.findOrdersByCriteria(name, address, status, pageable);
	}

	@GetMapping("/admin/order")
	public String getOrder(@RequestParam(name = "orderId") Optional<Integer> orderId, Model model,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "address", required = false) String address,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "page", defaultValue = "0") String pageStr,
			@RequestParam(value = "size", defaultValue = "5") String sizeStr) {
		int page = 0;
		int size = 5;

		try {
			page = Integer.parseInt(pageStr);
		} catch (NumberFormatException e) {

		}

		try {
			size = Integer.parseInt(sizeStr);
		} catch (NumberFormatException e) {

		}
		Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
		Page<Orders> ordersPage = orderJpa.findOrdersByCriteria(name, address, status, pageable);
		if (orderId.isPresent()) {
			Integer id = orderId.get();
			OrderDetails orderDetails = orderService.getOrderDetails(id);
			if (orderDetails != null) {
				List<OrderDetails> orderDetailList = orderService
						.findProductsByOrderId(orderId.get());
				Attributes currentSize = null;
				Attributes currentColor = null;
				BigDecimal subTotal = BigDecimal.ZERO;
				Orders order = orderDetails.getOrders();
				Coupons coupon = order.getCoupons();
				BigDecimal discountPercent = BigDecimal.ZERO;
				BigDecimal shipping = BigDecimal.valueOf(2);

				if (coupon != null) {
					discountPercent = coupon.getDiscountPercent();
				}
				for (OrderDetails detail : orderDetailList) {
					if (detail.getProducts().getProductId().equals(orderDetails.getProducts().getProductId())) {
						currentSize = detail.getColor();
						currentColor = detail.getSize();
					}
					int quantity = detail.getQuantity();
					BigDecimal retailPrice = detail.getProducts().getRetailPrice();
					BigDecimal totalPrice = retailPrice.multiply(BigDecimal.valueOf(quantity));
					subTotal = subTotal.add(totalPrice);

				}
				BigDecimal discount = subTotal.multiply(discountPercent).divide(BigDecimal.valueOf(100));
				BigDecimal total = subTotal.add(shipping).subtract(discount);

				NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
				String formattedSubTotal = currencyFormat.format(subTotal);
				String formattedShipping = currencyFormat.format(shipping);
				String formattedDiscount = currencyFormat.format(discount);
				String formattedTotal = currencyFormat.format(total);
				model.addAttribute("orderDetail", orderDetailList);
				model.addAttribute("orderDetails", orderDetails);
				model.addAttribute("currentSize", currentSize);
				model.addAttribute("currentColor", currentColor);
				model.addAttribute("subTotal", formattedSubTotal);
				model.addAttribute("discount", formattedDiscount);
				model.addAttribute("shipping", formattedShipping);
				model.addAttribute("total", formattedTotal);
				model.addAttribute("orders", ordersPage.getContent());
				model.addAttribute("currentPage", page);
				model.addAttribute("totalPages", ordersPage.getTotalPages());
				model.addAttribute("entries", size);
				model.addAttribute("name", name);
				model.addAttribute("addressFilter", address);
				model.addAttribute("statusFilter", status);
				return "Admin/html/orders";
			}

			return "Admin/html/orders";
		}

		model.addAttribute("orders", ordersPage.getContent());
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", ordersPage.getTotalPages());
		model.addAttribute("entries", size);
		model.addAttribute("name", name);
		model.addAttribute("addressFilter", address);
		model.addAttribute("statusFilter", status);
		return "Admin/html/orders";
	}

	@PostMapping("/admin/update/order")
	public String updateOrder(@RequestParam(name = "orderId") Optional<Integer> orderId,
			@RequestParam(name = "productId") Optional<Integer> productId,
			@RequestParam(name = "quantity") Optional<Integer> quantity,
			@RequestParam(name = "colorId") Optional<Integer> colorId,
			@RequestParam(name = "sizeId") Optional<Integer> sizeId, RedirectAttributes redirectAttributes) {

		if (orderId.isPresent() && productId.isPresent()) {
			Integer id = orderId.get();
			List<OrderDetails> orderDetailList = orderService.findProductsByOrderId(id);
			int getProductId = productId.get();

			for (OrderDetails orderDetails : orderDetailList) {
				if (orderDetails.getProducts().getProductId() == getProductId) {

					if (!colorId.isPresent()) {
						colorId = Optional.ofNullable(orderDetails.getColorId());
					}
					if (!sizeId.isPresent()) {
						sizeId = Optional.ofNullable(orderDetails.getSizeId());
					}

					if (quantity.isPresent()) {
						int newQuantity = quantity.get();
						if (newQuantity < 1) {
							redirectAttributes.addFlashAttribute("error",
									"Sorry! You cannot order a quantity less than 1. The product '"
											+ orderDetails.getProducts().getProductName()
											+ "' requires a minimum quantity of 1.");
							return "redirect:/admin/order?orderId=" + orderId.get();
						}

						int currentStockQuantity = orderDetails.getProducts().getStockQuantity();
						int currentOrderQuantity = orderDetails.getQuantity();
						int totalQuantityInStockAndQuantityCurrentOrder = currentStockQuantity + currentOrderQuantity;
						int newCurrentStockQuantity = totalQuantityInStockAndQuantityCurrentOrder - newQuantity;

						if (newQuantity <= totalQuantityInStockAndQuantityCurrentOrder) {
							orderDetails.setQuantity(newQuantity);
							orderDetails.getProducts().setStockQuantity(newCurrentStockQuantity);
						} else {
							redirectAttributes.addFlashAttribute("error",
									"Quantity exceeds current stock availability for product: "
											+ orderDetails.getProducts().getProductName());
							return "redirect:/admin/order?orderId=" + orderId.get();
						}

						updateAttributeProductQuantity(orderDetails.getProducts().getProductId(), colorId, sizeId,
								newQuantity, redirectAttributes);
					}

					if (colorId.isPresent()) {
						Integer newColorId = colorId.get();
						Optional<Attributes> newColorOpt = attributesService.findById(newColorId);
						if (newColorOpt.isPresent()) {
							orderDetails.setColor(newColorOpt.get());
						} else {
							redirectAttributes.addFlashAttribute("error", "Invalid color ID.");
							return "redirect:/admin/order?orderId=" + orderId.get();
						}
					}

					if (sizeId.isPresent()) {
						Integer newSizeId = sizeId.get();
						Optional<Attributes> newSizeOpt = attributesService.findById(newSizeId);
						if (newSizeOpt.isPresent()) {
							orderDetails.setSize(newSizeOpt.get());
						} else {
							redirectAttributes.addFlashAttribute("error", "Invalid size ID.");
							return "redirect:/admin/order?orderId=" + orderId.get();
						}
					}

					orderDetailJpa.save(orderDetails);
					break;
				}
			}

		} else {
			redirectAttributes.addFlashAttribute("error", "Missing order ID or product ID.");
			return "redirect:/admin/order?orderId=" + orderId.get();
		}

		return "redirect:/admin/order?orderId=" + orderId.get();
	}

	private void updateAttributeProductQuantity(int productId, Optional<Integer> colorId, Optional<Integer> sizeId,
			int newQuantity, RedirectAttributes redirectAttributes) {
		if (colorId.isPresent() && sizeId.isPresent()) {
			List<AttributeProduct> attributeProducts = attributeProductService.findByProductIdAndAttributeIds(productId,
					colorId.get(), sizeId.get());

			for (AttributeProduct attributeProduct : attributeProducts) {
				int currentAttributeProductQuantity = attributeProduct.getQuantity();
				int newAttributeProductQuantity = currentAttributeProductQuantity - newQuantity;

				if (newAttributeProductQuantity >= 0) {
					attributeProduct.setQuantity(newAttributeProductQuantity);
					attributeProductService.save(attributeProduct);
				} else {
					redirectAttributes.addFlashAttribute("error",
							"New quantity exceeds current stock for attribute product.");
					return;
				}
			}
		}
	}

	@PostMapping("admin/cancel/order")
	public String cancelOrder(@RequestParam("orderId") String orderId, RedirectAttributes redirectAttributes) {
		boolean isCancelled = orderService.updateOrderStatusToCancelled(orderId);
		if (isCancelled) {
			redirectAttributes.addFlashAttribute("success", "Order cancelled successfully.");
		} else {

			redirectAttributes.addFlashAttribute("error", "Failed to cancel order.");
		}

		return "redirect:/admin/order?orderId=" + orderId;
	}

	@PostMapping("admin/restore/order")
	public String restoreOrder(@RequestParam("orderId") String orderId, RedirectAttributes redirectAttributes) {
		boolean isRestored = orderService.restoreOrderStatus(orderId);
		if (isRestored) {
			redirectAttributes.addFlashAttribute("success", "Order restored successfully.");
		} else {
			redirectAttributes.addFlashAttribute("error", "Failed to restore order.");
		}

		return "redirect:/admin/order?orderId=" + orderId;
	}

	@GetMapping("/admin/order/export")
	public ResponseEntity<byte[]> exportToExcel(@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "address", required = false) String address,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "page", defaultValue = "0") String pageStr,
			@RequestParam(value = "size", defaultValue = "5") String sizeStr) {
		int page = 0;
		int size = 5;

		try {
			page = Integer.parseInt(pageStr);
		} catch (NumberFormatException e) {

		}

		try {
			size = Integer.parseInt(sizeStr);
		} catch (NumberFormatException e) {

		}
		Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
		Page<Orders> filteredOrders = orderJpa.findOrdersByCriteria(name, address, status, pageable);
		System.out.println(filteredOrders.getSize() + " Size");
		try (Workbook workbook = new XSSFWorkbook()) {

			Sheet sheet = workbook.createSheet("Orders");

			int rowNum = 0;
			for (Orders order : filteredOrders) {
				Row row = sheet.createRow(rowNum++);

				row.createCell(0).setCellValue(order.getOrderId());
				row.createCell(1).setCellValue(order.getUsers().getFirstName() + order.getUsers().getLastName());
				row.createCell(2).setCellValue(order.getUsers().getAddress().getAddressLine1());
				row.createCell(3).setCellValue(order.getOrderDate());
				row.createCell(4).setCellValue(order.getOrderStatus().getStatusName());
				row.createCell(5).setCellValue(String.valueOf(order.getTotalAmount()));
			}

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			workbook.write(outputStream);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			headers.setContentDispositionFormData("attachment", "orders.xlsx");

			return ResponseEntity.ok().headers(headers).body(outputStream.toByteArray());
		} catch (IOException e) {

			e.printStackTrace();

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}
}
