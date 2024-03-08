rootProject.name = "careinsurance"
include(
    ":api",
    ":batch",
    ":domain",
    ":domain:base",
    ":domain:message",
    ":domain:patch",
    ":domain:security",
    ":domain:user",
    ":domain:modification",
    ":domain:settlement",
    ":domain:settlement:settlementproperty",
    ":domain:settlement:settlementevent",
    ":domain:billing",
    ":domain:billing:billingproperty",
    ":domain:billing:billingevent",
    ":domain:caregiving",
    ":domain:patient",
    ":domain:phonenumber",
    ":domain:insurance",
    ":domain:file",
    ":domain:financial",
    ":domain:reconciliation",
    ":domain:reconciliation:reconciliationproperty",
    ":domain:reconciliation:reconciliationevent",
    ":domain:agency",
    ":ext",
    ":test",
    ":sql",
)
