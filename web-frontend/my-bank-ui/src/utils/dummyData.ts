const dummyCustomers: Customer[] = [
    {
        customerId: "cust001",
        user: {
            userId: "u001",
            firstName: "John",
            lastName: "Smith",
            gender: "Male",
            email: "john.smith@example.com",
            phone: "123-456-7890",
            address: "123 Main St",
            dateOfBirth: "1990-05-15",
            status: "Active",
        },
        createdAt: "2025-08-01",
    },
    {
        customerId: "cust002",
        user: {
            userId: "u003",
            firstName: "Alice",
            lastName: "Johnson",
            gender: "Female",
            email: "alice.johnson@example.com",
            phone: "555-123-4567",
            address: "789 Pine St",
            dateOfBirth: "1985-11-02",
            status: "Inactive",
        },
        createdAt: "2025-07-15",
    },
    // Add more as needed
];

const dummyUsers: User[] = [
    {
        userId: "u002",
        firstName: "Jane",
        lastName: "Doe",
        gender: "Female",
        email: "jane.doe@example.com",
        phone: "987-654-3210",
        address: "456 Elm St",
        dateOfBirth: "1992-09-20",
        status: "Pending",
    },
    {
        userId: "u004",
        firstName: "Bob",
        lastName: "Brown",
        gender: "Male",
        email: "bob.brown@example.com",
        phone: "321-654-0987",
        address: "321 Oak St",
        dateOfBirth: "1988-06-12",
        status: "Active",
    },
    // Add more as needed
];

export { dummyCustomers, dummyUsers };