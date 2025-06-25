/**
 * Domain layer package containing core business logic for order cancellations and returns.
 * 
 * <p>This package implements the domain layer of the hexagonal architecture and contains:</p>
 * 
 * <ul>
 *   <li><strong>Entities:</strong> Core business entities representing cancellation and return aggregates</li>
 *   <li><strong>Value Objects:</strong> Immutable objects representing domain concepts like RMA numbers, time windows, etc.</li>
 *   <li><strong>Services:</strong> Domain services implementing complex business logic</li>
 *   <li><strong>Ports:</strong> Interfaces defining contracts for external dependencies</li>
 *   <li><strong>Exceptions:</strong> Domain-specific exceptions for business rule violations</li>
 * </ul>
 * 
 * <p>The domain layer is completely independent of external frameworks and infrastructure concerns.
 * It defines business rules, invariants, and policies for the order cancellation and returns process.</p>
 * 
 * <p><strong>Key Business Rules Implemented:</strong></p>
 * <ul>
 *   <li>Cancellation window validation (time-based restrictions)</li>
 *   <li>Return period validation (delivery date + allowed days)</li>
 *   <li>Eligibility checking for cancellations and returns</li>
 *   <li>Refund calculation with business-specific fees and rules</li>
 *   <li>Inventory coordination for stock adjustments</li>
 *   <li>State transition validation for entities</li>
 * </ul>
 * 
 * @author Order Management System
 * @version 1.0
 * @since 1.0
 */
package ai.shreds.domain;