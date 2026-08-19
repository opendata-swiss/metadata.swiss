# User Acceptance Testing (UAT) - Process and Collaboration

This document describes the collaboration between the development team and the customer for verifying individual features.

| Feature Status | Additional Label | What it means in practice | Who owns it? |
| :--- | :--- | :--- | :--- |
| **In Progress** |  | Work is ongoing. | Developer |
| **Done** |  | Code is merged and feature is live in the REF environment. | Developer |
| **Ready for Review** |  | **Handover Point:** Feature passed internal QA, and is live in the ABN environment for the customer to test. | Developer (to signal) |
| **Ready for Review** | `BLOCKED` | **Defect Found:** A defect (bug) is preventing sign-off. The original feature stays in "Ready for Review" while the defect is fixed. | Customer (to report) |
| **Reviewed** |  | **Final Acceptance:** The feature is verified and signed off by the customer. | Customer |


---


## Handover Signaling

One of the biggest friction points in end-user testing is the customer trying to test something that isn't actually ready or deployed. To prevent this, we use the following steps to signal the **Handover**:

When a feature is done and live in the ABN environment for the customer to test, the developer does these two things:
1.  Move the ticket to the **Ready for Review** column.
2.  **Assign** the ticket to the Customer/Tester.



## Defect Reporting & Retest Loop

When the customer finds a defect, they follow these steps:
1.  **Create a new Issue for the defect:** Describe the defect, add screenshots and steps to reproduce the defect
2.  **Link it:** In the description or comments of the new issue, type `/relate #[Feature-Issue-ID]`. The defect now appears in the "Linked Items" section of the feature
3.  **Label the defect:** Add the **`Defect`** label.
4.  **Assign the defect** to the developer.
5.  **Flag the feature:** Go to the feature and add the **`BLOCKED`** label.

The original feature stays in "Ready for Review" while the defect is fixed. If a developer sees a ticket in "Ready for Review" with a `BLOCKED` label, they know testing has stalled.


* **Defect Lifecycle:** A defect ticket follows the same workflow as a feature: `To-Do` $\rightarrow$ `In Progress` $\rightarrow$ `Done`.

* **Retest Loop:** 1.  Developer fixes the defect and moves it to **Done**.
    2.  Developer assigns it back to the Tester/Customer.
    3.  Tester verifies the fix. If it’s fixed, they **Close** the defect.
    4.  When all defects on a feature are resolved, the original feature is Un-**`BLOCKED`**.


---


### Label Glossary

* **`Defect`**: Applied by the customer to any new issues created to report defects found during testing.
* **`BLOCKED`**: Applied by the customer to the original feature ticket to signal that a defect is preventing a features acceptance.
