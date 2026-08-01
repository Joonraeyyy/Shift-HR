package com.example.data.repository

import com.example.data.database.OrgNodeDao
import com.example.data.database.OrgNodeEntity
import kotlinx.coroutines.flow.Flow

class OrgRepository(private val orgNodeDao: OrgNodeDao) {

    val allOrgNodesFlow: Flow<List<OrgNodeEntity>> = orgNodeDao.getAllNodesFlow()

    suspend fun getAllNodes(): List<OrgNodeEntity> = orgNodeDao.getAllNodes()

    suspend fun insertNode(node: OrgNodeEntity) {
        orgNodeDao.insert(node)
    }

    suspend fun insertAll(nodes: List<OrgNodeEntity>) {
        orgNodeDao.insertAll(nodes)
    }

    suspend fun deleteNode(id: String) {
        orgNodeDao.deleteById(id)
    }

    suspend fun deleteAll() {
        orgNodeDao.deleteAll()
    }

    suspend fun seedInitialDataIfEmpty() {
        val existing = orgNodeDao.getAllNodes()
        if (existing.isEmpty()) {
            val initialNodes = listOf(
                // Level 1: General Manager / Executive Director
                OrgNodeEntity(
                    id = "NODE-GM-01",
                    name = "Elena Vance",
                    role = "GENERAL_MANAGER",
                    department = "Operations & Logistics",
                    parentId = null,
                    positionX = 115f,
                    positionY = 20f,
                    assignedShift = "Executive Schedule",
                    attendanceStatus = "CLOCKED_IN_GEOFENCE",
                    weeklyHours = 42.0,
                    restPeriodHours = 14.0,
                    approvalTier = 3
                ),

                // Level 2: Engineering & Tech Supervisor
                OrgNodeEntity(
                    id = "NODE-SUP-01",
                    name = "Marcus Vance",
                    role = "SUPERVISOR",
                    department = "Engineering & Tech",
                    parentId = "NODE-GM-01",
                    positionX = 20f,
                    positionY = 150f,
                    assignedShift = "Day Shift (08:00 - 16:00)",
                    attendanceStatus = "CLOCKED_IN_GEOFENCE",
                    weeklyHours = 44.5,
                    restPeriodHours = 12.5,
                    approvalTier = 2
                ),

                // Level 2: Operations & Logistics Supervisor
                OrgNodeEntity(
                    id = "NODE-SUP-02",
                    name = "Sophia Martinez",
                    role = "SUPERVISOR",
                    department = "Operations & Logistics",
                    parentId = "NODE-GM-01",
                    positionX = 220f,
                    positionY = 150f,
                    assignedShift = "Day Shift (08:00 - 16:00)",
                    attendanceStatus = "CLOCKED_IN_GEOFENCE",
                    weeklyHours = 38.0,
                    restPeriodHours = 13.0,
                    approvalTier = 2
                ),

                // Level 2: HR & Finance Supervisor
                OrgNodeEntity(
                    id = "NODE-SUP-03",
                    name = "David Chen",
                    role = "SUPERVISOR",
                    department = "Human Resources",
                    parentId = "NODE-GM-01",
                    positionX = 420f,
                    positionY = 150f,
                    assignedShift = "Day Shift (08:00 - 16:00)",
                    attendanceStatus = "ON_BREAK",
                    weeklyHours = 40.0,
                    restPeriodHours = 15.0,
                    approvalTier = 2
                ),

                // Level 3: Engineering Employees
                OrgNodeEntity(
                    id = "NODE-EMP-01",
                    name = "Liam O'Connor",
                    role = "EMPLOYEE",
                    department = "Engineering & Tech",
                    parentId = "NODE-SUP-01",
                    positionX = 10f,
                    positionY = 300f,
                    assignedShift = "Day Shift (08:00 - 16:00)",
                    attendanceStatus = "CLOCKED_IN_GEOFENCE",
                    weeklyHours = 39.0,
                    restPeriodHours = 11.5,
                    approvalTier = 1
                ),
                OrgNodeEntity(
                    id = "NODE-EMP-02",
                    name = "Aria Patel",
                    role = "EMPLOYEE",
                    department = "Engineering & Tech",
                    parentId = "NODE-SUP-01",
                    positionX = 130f,
                    positionY = 300f,
                    assignedShift = "Day Shift (08:00 - 16:00)",
                    attendanceStatus = "CLOCKED_IN_GEOFENCE",
                    weeklyHours = 37.5,
                    restPeriodHours = 12.0,
                    approvalTier = 1
                ),

                // Level 3: Operations Employees
                OrgNodeEntity(
                    id = "NODE-EMP-03",
                    name = "Ethan Wright",
                    role = "EMPLOYEE",
                    department = "Operations & Logistics",
                    parentId = "NODE-SUP-02",
                    positionX = 250f,
                    positionY = 300f,
                    assignedShift = "Night Shift (16:00 - 00:00)",
                    attendanceStatus = "OFF_DUTY",
                    weeklyHours = 35.0,
                    restPeriodHours = 16.0,
                    approvalTier = 1
                ),
                OrgNodeEntity(
                    id = "NODE-EMP-04",
                    name = "Chloe Dubois",
                    role = "EMPLOYEE",
                    department = "Operations & Logistics",
                    parentId = "NODE-SUP-02",
                    positionX = 370f,
                    positionY = 300f,
                    assignedShift = "Night Shift (16:00 - 00:00)",
                    attendanceStatus = "CLOCKED_IN_GEOFENCE",
                    weeklyHours = 41.0,
                    restPeriodHours = 10.5,
                    approvalTier = 1
                ),

                // Level 3: HR Employees
                OrgNodeEntity(
                    id = "NODE-EMP-05",
                    name = "Noah Kim",
                    role = "EMPLOYEE",
                    department = "Human Resources",
                    parentId = "NODE-SUP-03",
                    positionX = 490f,
                    positionY = 300f,
                    assignedShift = "Day Shift (08:00 - 16:00)",
                    attendanceStatus = "CLOCKED_IN_GEOFENCE",
                    weeklyHours = 38.0,
                    restPeriodHours = 14.0,
                    approvalTier = 1
                )
            )
            orgNodeDao.insertAll(initialNodes)
        }
    }
}
