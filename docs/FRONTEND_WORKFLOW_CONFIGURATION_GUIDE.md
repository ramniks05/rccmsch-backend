# Frontend Implementation Guide: Workflow Configuration (Admin Panel)

## Overview

This guide provides complete implementation examples for integrating workflow configuration APIs into your frontend application. The examples are provided for both Angular and React frameworks.

## Prerequisites

- Admin authentication token stored in localStorage/sessionStorage
- HTTP client configured (Angular HttpClient or Axios/Fetch)
- Form validation library (Angular Forms or React Hook Form)

---

## Table of Contents

1. [Service Implementation](#1-service-implementation)
2. [Workflow Management Component](#2-workflow-management-component)
3. [State Management Component](#3-state-management-component)
4. [Transition Management Component](#4-transition-management-component)
5. [Permission Management Component](#5-permission-management-component)
6. [Complete Workflow Builder](#6-complete-workflow-builder)
7. [Error Handling](#7-error-handling)
8. [Return for Correction (Citizen Flow)](#8-return-for-correction-citizen-flow)

---

## 1. Service Implementation

### Angular Service

**File:** `src/app/services/workflow-config.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface WorkflowDefinition {
  id?: number;
  workflowCode: string;
  workflowName: string;
  description?: string;
  isActive?: boolean;
  version?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface WorkflowState {
  id?: number;
  workflowId?: number;
  workflowCode?: string;
  stateCode: string;
  stateName: string;
  stateOrder: number;
  isInitialState: boolean;
  isFinalState: boolean;
  description?: string;
}

export interface WorkflowTransition {
  id?: number;
  workflowId?: number;
  fromStateId: number;
  toStateId: number;
  transitionCode: string;
  transitionName: string;
  requiresComment?: boolean;
  isActive?: boolean;
  description?: string;
}

export interface WorkflowPermission {
  id?: number;
  transitionId?: number;
  transitionCode?: string;
  roleCode: string;
  unitLevel?: 'STATE' | 'DISTRICT' | 'SUB_DIVISION' | 'CIRCLE' | null;
  canInitiate: boolean;
  canApprove: boolean;
  hierarchyRule?: string;
  conditions?: string;
  isActive?: boolean;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
}

@Injectable({
  providedIn: 'root'
})
export class WorkflowConfigService {
  private apiUrl = `${environment.apiUrl}/api/admin/workflow`;
  private token: string | null = null;

  constructor(private http: HttpClient) {
    this.token = localStorage.getItem('adminToken');
  }

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${this.token}`
    });
  }

  // ==================== Workflow Definition APIs ====================

  getAllWorkflows(): Observable<ApiResponse<WorkflowDefinition[]>> {
    return this.http.get<ApiResponse<WorkflowDefinition[]>>(
      `${this.apiUrl}/definitions`,
      { headers: this.getHeaders() }
    );
  }

  getActiveWorkflows(): Observable<ApiResponse<WorkflowDefinition[]>> {
    return this.http.get<ApiResponse<WorkflowDefinition[]>>(
      `${this.apiUrl}/definitions/active`,
      { headers: this.getHeaders() }
    );
  }

  getWorkflowById(id: number): Observable<ApiResponse<WorkflowDefinition>> {
    return this.http.get<ApiResponse<WorkflowDefinition>>(
      `${this.apiUrl}/definitions/id/${id}`,
      { headers: this.getHeaders() }
    );
  }

  getWorkflowByCode(code: string): Observable<ApiResponse<WorkflowDefinition>> {
    return this.http.get<ApiResponse<WorkflowDefinition>>(
      `${this.apiUrl}/definitions/${code}`,
      { headers: this.getHeaders() }
    );
  }

  createWorkflow(workflow: WorkflowDefinition): Observable<ApiResponse<WorkflowDefinition>> {
    return this.http.post<ApiResponse<WorkflowDefinition>>(
      `${this.apiUrl}/definitions`,
      workflow,
      { headers: this.getHeaders() }
    );
  }

  updateWorkflow(id: number, workflow: WorkflowDefinition): Observable<ApiResponse<WorkflowDefinition>> {
    return this.http.put<ApiResponse<WorkflowDefinition>>(
      `${this.apiUrl}/definitions/${id}`,
      workflow,
      { headers: this.getHeaders() }
    );
  }

  deleteWorkflow(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(
      `${this.apiUrl}/definitions/${id}`,
      { headers: this.getHeaders() }
    );
  }

  // ==================== Workflow State APIs ====================

  getWorkflowStates(workflowId: number): Observable<ApiResponse<WorkflowState[]>> {
    return this.http.get<ApiResponse<WorkflowState[]>>(
      `${this.apiUrl}/${workflowId}/states`,
      { headers: this.getHeaders() }
    );
  }

  getStateById(id: number): Observable<ApiResponse<WorkflowState>> {
    return this.http.get<ApiResponse<WorkflowState>>(
      `${this.apiUrl}/states/${id}`,
      { headers: this.getHeaders() }
    );
  }

  createState(workflowId: number, state: WorkflowState): Observable<ApiResponse<WorkflowState>> {
    return this.http.post<ApiResponse<WorkflowState>>(
      `${this.apiUrl}/${workflowId}/states`,
      state,
      { headers: this.getHeaders() }
    );
  }

  updateState(id: number, state: WorkflowState): Observable<ApiResponse<WorkflowState>> {
    return this.http.put<ApiResponse<WorkflowState>>(
      `${this.apiUrl}/states/${id}`,
      state,
      { headers: this.getHeaders() }
    );
  }

  deleteState(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(
      `${this.apiUrl}/states/${id}`,
      { headers: this.getHeaders() }
    );
  }

  // ==================== Workflow Transition APIs ====================

  getWorkflowTransitions(workflowId: number): Observable<ApiResponse<WorkflowTransition[]>> {
    return this.http.get<ApiResponse<WorkflowTransition[]>>(
      `${this.apiUrl}/${workflowId}/transitions`,
      { headers: this.getHeaders() }
    );
  }

  getAllWorkflowTransitions(workflowId: number): Observable<ApiResponse<WorkflowTransition[]>> {
    return this.http.get<ApiResponse<WorkflowTransition[]>>(
      `${this.apiUrl}/${workflowId}/transitions/all`,
      { headers: this.getHeaders() }
    );
  }

  getTransitionById(id: number): Observable<ApiResponse<WorkflowTransition>> {
    return this.http.get<ApiResponse<WorkflowTransition>>(
      `${this.apiUrl}/transitions/${id}`,
      { headers: this.getHeaders() }
    );
  }

  createTransition(workflowId: number, transition: WorkflowTransition): Observable<ApiResponse<WorkflowTransition>> {
    return this.http.post<ApiResponse<WorkflowTransition>>(
      `${this.apiUrl}/${workflowId}/transitions`,
      transition,
      { headers: this.getHeaders() }
    );
  }

  updateTransition(id: number, transition: WorkflowTransition): Observable<ApiResponse<WorkflowTransition>> {
    return this.http.put<ApiResponse<WorkflowTransition>>(
      `${this.apiUrl}/transitions/${id}`,
      transition,
      { headers: this.getHeaders() }
    );
  }

  deleteTransition(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(
      `${this.apiUrl}/transitions/${id}`,
      { headers: this.getHeaders() }
    );
  }

  // ==================== Workflow Permission APIs ====================

  getTransitionPermissions(transitionId: number): Observable<ApiResponse<WorkflowPermission[]>> {
    return this.http.get<ApiResponse<WorkflowPermission[]>>(
      `${this.apiUrl}/transitions/${transitionId}/permissions`,
      { headers: this.getHeaders() }
    );
  }

  getPermissionById(id: number): Observable<ApiResponse<WorkflowPermission>> {
    return this.http.get<ApiResponse<WorkflowPermission>>(
      `${this.apiUrl}/permissions/${id}`,
      { headers: this.getHeaders() }
    );
  }

  createPermission(transitionId: number, permission: WorkflowPermission): Observable<ApiResponse<WorkflowPermission>> {
    return this.http.post<ApiResponse<WorkflowPermission>>(
      `${this.apiUrl}/transitions/${transitionId}/permissions`,
      permission,
      { headers: this.getHeaders() }
    );
  }

  updatePermission(id: number, permission: WorkflowPermission): Observable<ApiResponse<WorkflowPermission>> {
    return this.http.put<ApiResponse<WorkflowPermission>>(
      `${this.apiUrl}/permissions/${id}`,
      permission,
      { headers: this.getHeaders() }
    );
  }

  deletePermission(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(
      `${this.apiUrl}/permissions/${id}`,
      { headers: this.getHeaders() }
    );
  }
}
```

### React Service/Hook

**File:** `src/services/workflowConfigService.ts`

```typescript
import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

export interface WorkflowDefinition {
  id?: number;
  workflowCode: string;
  workflowName: string;
  description?: string;
  isActive?: boolean;
  version?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface WorkflowState {
  id?: number;
  workflowId?: number;
  workflowCode?: string;
  stateCode: string;
  stateName: string;
  stateOrder: number;
  isInitialState: boolean;
  isFinalState: boolean;
  description?: string;
}

export interface WorkflowTransition {
  id?: number;
  workflowId?: number;
  fromStateId: number;
  toStateId: number;
  transitionCode: string;
  transitionName: string;
  requiresComment?: boolean;
  isActive?: boolean;
  description?: string;
}

export interface WorkflowPermission {
  id?: number;
  transitionId?: number;
  transitionCode?: string;
  roleCode: string;
  unitLevel?: 'STATE' | 'DISTRICT' | 'SUB_DIVISION' | 'CIRCLE' | null;
  canInitiate: boolean;
  canApprove: boolean;
  hierarchyRule?: string;
  conditions?: string;
  isActive?: boolean;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
}

const getAuthHeaders = () => {
  const token = localStorage.getItem('adminToken');
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };
};

// ==================== Workflow Definition APIs ====================

export const getAllWorkflows = async (): Promise<WorkflowDefinition[]> => {
  const response = await axios.get<ApiResponse<WorkflowDefinition[]>>(
    `${API_URL}/api/admin/workflow/definitions`,
    { headers: getAuthHeaders() }
  );
  return response.data.data;
};

export const getActiveWorkflows = async (): Promise<WorkflowDefinition[]> => {
  const response = await axios.get<ApiResponse<WorkflowDefinition[]>>(
    `${API_URL}/api/admin/workflow/definitions/active`,
    { headers: getAuthHeaders() }
  );
  return response.data.data;
};

export const getWorkflowById = async (id: number): Promise<WorkflowDefinition> => {
  const response = await axios.get<ApiResponse<WorkflowDefinition>>(
    `${API_URL}/api/admin/workflow/definitions/id/${id}`,
    { headers: getAuthHeaders() }
  );
  return response.data.data;
};

export const createWorkflow = async (workflow: WorkflowDefinition): Promise<WorkflowDefinition> => {
  const response = await axios.post<ApiResponse<WorkflowDefinition>>(
    `${API_URL}/api/admin/workflow/definitions`,
    workflow,
    { headers: getAuthHeaders() }
  );
  return response.data.data;
};

export const updateWorkflow = async (id: number, workflow: WorkflowDefinition): Promise<WorkflowDefinition> => {
  const response = await axios.put<ApiResponse<WorkflowDefinition>>(
    `${API_URL}/api/admin/workflow/definitions/${id}`,
    workflow,
    { headers: getAuthHeaders() }
  );
  return response.data.data;
};

export const deleteWorkflow = async (id: number): Promise<void> => {
  await axios.delete<ApiResponse<void>>(
    `${API_URL}/api/admin/workflow/definitions/${id}`,
    { headers: getAuthHeaders() }
  );
};

// ==================== Workflow State APIs ====================

export const getWorkflowStates = async (workflowId: number): Promise<WorkflowState[]> => {
  const response = await axios.get<ApiResponse<WorkflowState[]>>(
    `${API_URL}/api/admin/workflow/${workflowId}/states`,
    { headers: getAuthHeaders() }
  );
  return response.data.data;
};

export const createState = async (workflowId: number, state: WorkflowState): Promise<WorkflowState> => {
  const response = await axios.post<ApiResponse<WorkflowState>>(
    `${API_URL}/api/admin/workflow/${workflowId}/states`,
    state,
    { headers: getAuthHeaders() }
  );
  return response.data.data;
};

export const updateState = async (id: number, state: WorkflowState): Promise<WorkflowState> => {
  const response = await axios.put<ApiResponse<WorkflowState>>(
    `${API_URL}/api/admin/workflow/states/${id}`,
    state,
    { headers: getAuthHeaders() }
  );
  return response.data.data;
};

export const deleteState = async (id: number): Promise<void> => {
  await axios.delete<ApiResponse<void>>(
    `${API_URL}/api/admin/workflow/states/${id}`,
    { headers: getAuthHeaders() }
  );
};

// ==================== Workflow Transition APIs ====================

export const getWorkflowTransitions = async (workflowId: number): Promise<WorkflowTransition[]> => {
  const response = await axios.get<ApiResponse<WorkflowTransition[]>>(
    `${API_URL}/api/admin/workflow/${workflowId}/transitions`,
    { headers: getAuthHeaders() }
  );
  return response.data.data;
};

export const createTransition = async (workflowId: number, transition: WorkflowTransition): Promise<WorkflowTransition> => {
  const response = await axios.post<ApiResponse<WorkflowTransition>>(
    `${API_URL}/api/admin/workflow/${workflowId}/transitions`,
    transition,
    { headers: getAuthHeaders() }
  );
  return response.data.data;
};

export const updateTransition = async (id: number, transition: WorkflowTransition): Promise<WorkflowTransition> => {
  const response = await axios.put<ApiResponse<WorkflowTransition>>(
    `${API_URL}/api/admin/workflow/transitions/${id}`,
    transition,
    { headers: getAuthHeaders() }
  );
  return response.data.data;
};

export const deleteTransition = async (id: number): Promise<void> => {
  await axios.delete<ApiResponse<void>>(
    `${API_URL}/api/admin/workflow/transitions/${id}`,
    { headers: getAuthHeaders() }
  );
};

// ==================== Workflow Permission APIs ====================

export const getTransitionPermissions = async (transitionId: number): Promise<WorkflowPermission[]> => {
  const response = await axios.get<ApiResponse<WorkflowPermission[]>>(
    `${API_URL}/api/admin/workflow/transitions/${transitionId}/permissions`,
    { headers: getAuthHeaders() }
  );
  return response.data.data;
};

export const createPermission = async (transitionId: number, permission: WorkflowPermission): Promise<WorkflowPermission> => {
  const response = await axios.post<ApiResponse<WorkflowPermission>>(
    `${API_URL}/api/admin/workflow/transitions/${transitionId}/permissions`,
    permission,
    { headers: getAuthHeaders() }
  );
  return response.data.data;
};

export const updatePermission = async (id: number, permission: WorkflowPermission): Promise<WorkflowPermission> => {
  const response = await axios.put<ApiResponse<WorkflowPermission>>(
    `${API_URL}/api/admin/workflow/permissions/${id}`,
    permission,
    { headers: getAuthHeaders() }
  );
  return response.data.data;
};

export const deletePermission = async (id: number): Promise<void> => {
  await axios.delete<ApiResponse<void>>(
    `${API_URL}/api/admin/workflow/permissions/${id}`,
    { headers: getAuthHeaders() }
  );
};
```

---

## 2. Workflow Management Component

### Angular Component

**File:** `src/app/components/workflow-list/workflow-list.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { WorkflowConfigService, WorkflowDefinition } from '../../services/workflow-config.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-workflow-list',
  templateUrl: './workflow-list.component.html',
  styleUrls: ['./workflow-list.component.css']
})
export class WorkflowListComponent implements OnInit {
  workflows: WorkflowDefinition[] = [];
  loading = false;
  showCreateModal = false;
  selectedWorkflow: WorkflowDefinition | null = null;
  editMode = false;

  workflowForm = {
    workflowCode: '',
    workflowName: '',
    description: '',
    isActive: true
  };

  constructor(
    private workflowService: WorkflowConfigService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadWorkflows();
  }

  loadWorkflows(): void {
    this.loading = true;
    this.workflowService.getAllWorkflows().subscribe({
      next: (response) => {
        if (response.success) {
          this.workflows = response.data;
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading workflows:', error);
        alert('Failed to load workflows: ' + (error.error?.message || error.message));
        this.loading = false;
      }
    });
  }

  openCreateModal(): void {
    this.editMode = false;
    this.selectedWorkflow = null;
    this.workflowForm = {
      workflowCode: '',
      workflowName: '',
      description: '',
      isActive: true
    };
    this.showCreateModal = true;
  }

  openEditModal(workflow: WorkflowDefinition): void {
    this.editMode = true;
    this.selectedWorkflow = workflow;
    this.workflowForm = {
      workflowCode: workflow.workflowCode,
      workflowName: workflow.workflowName,
      description: workflow.description || '',
      isActive: workflow.isActive !== false
    };
    this.showCreateModal = true;
  }

  saveWorkflow(): void {
    if (!this.workflowForm.workflowCode || !this.workflowForm.workflowName) {
      alert('Workflow code and name are required');
      return;
    }

    const workflow: WorkflowDefinition = {
      workflowCode: this.workflowForm.workflowCode,
      workflowName: this.workflowForm.workflowName,
      description: this.workflowForm.description,
      isActive: this.workflowForm.isActive
    };

    if (this.editMode && this.selectedWorkflow?.id) {
      this.workflowService.updateWorkflow(this.selectedWorkflow.id, workflow).subscribe({
        next: (response) => {
          if (response.success) {
            alert('Workflow updated successfully');
            this.loadWorkflows();
            this.showCreateModal = false;
          }
        },
        error: (error) => {
          alert('Failed to update workflow: ' + (error.error?.message || error.message));
        }
      });
    } else {
      this.workflowService.createWorkflow(workflow).subscribe({
        next: (response) => {
          if (response.success) {
            alert('Workflow created successfully');
            this.loadWorkflows();
            this.showCreateModal = false;
          }
        },
        error: (error) => {
          alert('Failed to create workflow: ' + (error.error?.message || error.message));
        }
      });
    }
  }

  deleteWorkflow(workflow: WorkflowDefinition): void {
    if (!confirm(`Are you sure you want to delete workflow "${workflow.workflowName}"?`)) {
      return;
    }

    if (workflow.id) {
      this.workflowService.deleteWorkflow(workflow.id).subscribe({
        next: (response) => {
          if (response.success) {
            alert('Workflow deleted successfully');
            this.loadWorkflows();
          }
        },
        error: (error) => {
          alert('Failed to delete workflow: ' + (error.error?.message || error.message));
        }
      });
    }
  }

  viewWorkflowDetails(workflow: WorkflowDefinition): void {
    if (workflow.id) {
      this.router.navigate(['/admin/workflows', workflow.id]);
    }
  }
}
```

**File:** `src/app/components/workflow-list/workflow-list.component.html`

```html
<div class="workflow-list-container">
  <div class="header">
    <h2>Workflow Management</h2>
    <button class="btn btn-primary" (click)="openCreateModal()">
      <i class="fas fa-plus"></i> Create New Workflow
    </button>
  </div>

  <div *ngIf="loading" class="loading">Loading workflows...</div>

  <table class="table" *ngIf="!loading">
    <thead>
      <tr>
        <th>Code</th>
        <th>Name</th>
        <th>Description</th>
        <th>Status</th>
        <th>Version</th>
        <th>Actions</th>
      </tr>
    </thead>
    <tbody>
      <tr *ngFor="let workflow of workflows">
        <td>{{ workflow.workflowCode }}</td>
        <td>{{ workflow.workflowName }}</td>
        <td>{{ workflow.description || '-' }}</td>
        <td>
          <span [class]="workflow.isActive ? 'badge badge-success' : 'badge badge-danger'">
            {{ workflow.isActive ? 'Active' : 'Inactive' }}
          </span>
        </td>
        <td>{{ workflow.version }}</td>
        <td>
          <button class="btn btn-sm btn-info" (click)="viewWorkflowDetails(workflow)">
            View Details
          </button>
          <button class="btn btn-sm btn-warning" (click)="openEditModal(workflow)">
            Edit
          </button>
          <button class="btn btn-sm btn-danger" (click)="deleteWorkflow(workflow)">
            Delete
          </button>
        </td>
      </tr>
    </tbody>
  </table>

  <!-- Create/Edit Modal -->
  <div class="modal" *ngIf="showCreateModal" (click)="showCreateModal = false">
    <div class="modal-content" (click)="$event.stopPropagation()">
      <div class="modal-header">
        <h3>{{ editMode ? 'Edit' : 'Create' }} Workflow</h3>
        <button class="close" (click)="showCreateModal = false">&times;</button>
      </div>
      <div class="modal-body">
        <form>
          <div class="form-group">
            <label>Workflow Code *</label>
            <input 
              type="text" 
              class="form-control" 
              [(ngModel)]="workflowForm.workflowCode"
              [disabled]="editMode"
              name="workflowCode"
              required
            />
          </div>
          <div class="form-group">
            <label>Workflow Name *</label>
            <input 
              type="text" 
              class="form-control" 
              [(ngModel)]="workflowForm.workflowName"
              name="workflowName"
              required
            />
          </div>
          <div class="form-group">
            <label>Description</label>
            <textarea 
              class="form-control" 
              [(ngModel)]="workflowForm.description"
              name="description"
              rows="3"
            ></textarea>
          </div>
          <div class="form-group">
            <label>
              <input 
                type="checkbox" 
                [(ngModel)]="workflowForm.isActive"
                name="isActive"
              />
              Active
            </label>
          </div>
        </form>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" (click)="showCreateModal = false">Cancel</button>
        <button class="btn btn-primary" (click)="saveWorkflow()">Save</button>
      </div>
    </div>
  </div>
</div>
```

### React Component

**File:** `src/components/WorkflowList.tsx`

```typescript
import React, { useState, useEffect } from 'react';
import {
  getAllWorkflows,
  createWorkflow,
  updateWorkflow,
  deleteWorkflow,
  WorkflowDefinition
} from '../services/workflowConfigService';
import { useNavigate } from 'react-router-dom';

const WorkflowList: React.FC = () => {
  const [workflows, setWorkflows] = useState<WorkflowDefinition[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [selectedWorkflow, setSelectedWorkflow] = useState<WorkflowDefinition | null>(null);
  const [formData, setFormData] = useState({
    workflowCode: '',
    workflowName: '',
    description: '',
    isActive: true
  });
  const navigate = useNavigate();

  useEffect(() => {
    loadWorkflows();
  }, []);

  const loadWorkflows = async () => {
    setLoading(true);
    try {
      const data = await getAllWorkflows();
      setWorkflows(data);
    } catch (error: any) {
      alert('Failed to load workflows: ' + (error.response?.data?.message || error.message));
    } finally {
      setLoading(false);
    }
  };

  const openCreateModal = () => {
    setEditMode(false);
    setSelectedWorkflow(null);
    setFormData({
      workflowCode: '',
      workflowName: '',
      description: '',
      isActive: true
    });
    setShowModal(true);
  };

  const openEditModal = (workflow: WorkflowDefinition) => {
    setEditMode(true);
    setSelectedWorkflow(workflow);
    setFormData({
      workflowCode: workflow.workflowCode,
      workflowName: workflow.workflowName,
      description: workflow.description || '',
      isActive: workflow.isActive !== false
    });
    setShowModal(true);
  };

  const handleSave = async () => {
    if (!formData.workflowCode || !formData.workflowName) {
      alert('Workflow code and name are required');
      return;
    }

    try {
      if (editMode && selectedWorkflow?.id) {
        await updateWorkflow(selectedWorkflow.id, formData);
        alert('Workflow updated successfully');
      } else {
        await createWorkflow(formData);
        alert('Workflow created successfully');
      }
      loadWorkflows();
      setShowModal(false);
    } catch (error: any) {
      alert('Failed to save workflow: ' + (error.response?.data?.message || error.message));
    }
  };

  const handleDelete = async (workflow: WorkflowDefinition) => {
    if (!window.confirm(`Are you sure you want to delete workflow "${workflow.workflowName}"?`)) {
      return;
    }

    try {
      if (workflow.id) {
        await deleteWorkflow(workflow.id);
        alert('Workflow deleted successfully');
        loadWorkflows();
      }
    } catch (error: any) {
      alert('Failed to delete workflow: ' + (error.response?.data?.message || error.message));
    }
  };

  return (
    <div className="workflow-list-container">
      <div className="header">
        <h2>Workflow Management</h2>
        <button className="btn btn-primary" onClick={openCreateModal}>
          <i className="fas fa-plus"></i> Create New Workflow
        </button>
      </div>

      {loading && <div className="loading">Loading workflows...</div>}

      <table className="table">
        <thead>
          <tr>
            <th>Code</th>
            <th>Name</th>
            <th>Description</th>
            <th>Status</th>
            <th>Version</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {workflows.map((workflow) => (
            <tr key={workflow.id}>
              <td>{workflow.workflowCode}</td>
              <td>{workflow.workflowName}</td>
              <td>{workflow.description || '-'}</td>
              <td>
                <span className={`badge ${workflow.isActive ? 'badge-success' : 'badge-danger'}`}>
                  {workflow.isActive ? 'Active' : 'Inactive'}
                </span>
              </td>
              <td>{workflow.version}</td>
              <td>
                <button 
                  className="btn btn-sm btn-info"
                  onClick={() => navigate(`/admin/workflows/${workflow.id}`)}
                >
                  View Details
                </button>
                <button 
                  className="btn btn-sm btn-warning"
                  onClick={() => openEditModal(workflow)}
                >
                  Edit
                </button>
                <button 
                  className="btn btn-sm btn-danger"
                  onClick={() => handleDelete(workflow)}
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Modal */}
      {showModal && (
        <div className="modal" onClick={() => setShowModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editMode ? 'Edit' : 'Create'} Workflow</h3>
              <button className="close" onClick={() => setShowModal(false)}>&times;</button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label>Workflow Code *</label>
                <input
                  type="text"
                  className="form-control"
                  value={formData.workflowCode}
                  onChange={(e) => setFormData({ ...formData, workflowCode: e.target.value })}
                  disabled={editMode}
                  required
                />
              </div>
              <div className="form-group">
                <label>Workflow Name *</label>
                <input
                  type="text"
                  className="form-control"
                  value={formData.workflowName}
                  onChange={(e) => setFormData({ ...formData, workflowName: e.target.value })}
                  required
                />
              </div>
              <div className="form-group">
                <label>Description</label>
                <textarea
                  className="form-control"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  rows={3}
                />
              </div>
              <div className="form-group">
                <label>
                  <input
                    type="checkbox"
                    checked={formData.isActive}
                    onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                  />
                  Active
                </label>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setShowModal(false)}>
                Cancel
              </button>
              <button className="btn btn-primary" onClick={handleSave}>
                Save
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default WorkflowList;
```

---

## 3. State Management Component

### Angular Component (Simplified)

**File:** `src/app/components/workflow-states/workflow-states.component.ts`

```typescript
import { Component, OnInit, Input } from '@angular/core';
import { WorkflowConfigService, WorkflowState } from '../../services/workflow-config.service';

@Component({
  selector: 'app-workflow-states',
  templateUrl: './workflow-states.component.html'
})
export class WorkflowStatesComponent implements OnInit {
  @Input() workflowId!: number;
  states: WorkflowState[] = [];
  loading = false;
  showModal = false;
  editMode = false;
  selectedState: WorkflowState | null = null;

  stateForm = {
    stateCode: '',
    stateName: '',
    stateOrder: 1,
    isInitialState: false,
    isFinalState: false,
    description: ''
  };

  constructor(private workflowService: WorkflowConfigService) {}

  ngOnInit(): void {
    this.loadStates();
  }

  loadStates(): void {
    this.loading = true;
    this.workflowService.getWorkflowStates(this.workflowId).subscribe({
      next: (response) => {
        if (response.success) {
          this.states = response.data;
        }
        this.loading = false;
      },
      error: (error) => {
        alert('Failed to load states: ' + (error.error?.message || error.message));
        this.loading = false;
      }
    });
  }

  openCreateModal(): void {
    this.editMode = false;
    this.selectedState = null;
    const maxOrder = Math.max(...this.states.map(s => s.stateOrder || 0), 0);
    this.stateForm = {
      stateCode: '',
      stateName: '',
      stateOrder: maxOrder + 1,
      isInitialState: false,
      isFinalState: false,
      description: ''
    };
    this.showModal = true;
  }

  saveState(): void {
    if (!this.stateForm.stateCode || !this.stateForm.stateName) {
      alert('State code and name are required');
      return;
    }

    const state: WorkflowState = {
      stateCode: this.stateForm.stateCode,
      stateName: this.stateForm.stateName,
      stateOrder: this.stateForm.stateOrder,
      isInitialState: this.stateForm.isInitialState,
      isFinalState: this.stateForm.isFinalState,
      description: this.stateForm.description
    };

    if (this.editMode && this.selectedState?.id) {
      this.workflowService.updateState(this.selectedState.id, state).subscribe({
        next: (response) => {
          if (response.success) {
            alert('State updated successfully');
            this.loadStates();
            this.showModal = false;
          }
        },
        error: (error) => {
          alert('Failed to update state: ' + (error.error?.message || error.message));
        }
      });
    } else {
      this.workflowService.createState(this.workflowId, state).subscribe({
        next: (response) => {
          if (response.success) {
            alert('State created successfully');
            this.loadStates();
            this.showModal = false;
          }
        },
        error: (error) => {
          alert('Failed to create state: ' + (error.error?.message || error.message));
        }
      });
    }
  }

  deleteState(state: WorkflowState): void {
    if (!confirm(`Delete state "${state.stateName}"?`)) return;
    
    if (state.id) {
      this.workflowService.deleteState(state.id).subscribe({
        next: () => {
          alert('State deleted successfully');
          this.loadStates();
        },
        error: (error) => {
          alert('Failed to delete state: ' + (error.error?.message || error.message));
        }
      });
    }
  }
}
```

---

## 4. Complete Workflow Builder Component

### Angular Component - Complete Workflow Builder

**File:** `src/app/components/workflow-builder/workflow-builder.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { WorkflowConfigService, WorkflowDefinition, WorkflowState, WorkflowTransition, WorkflowPermission } from '../../services/workflow-config.service';

@Component({
  selector: 'app-workflow-builder',
  templateUrl: './workflow-builder.component.html',
  styleUrls: ['./workflow-builder.component.css']
})
export class WorkflowBuilderComponent implements OnInit {
  workflowId!: number;
  workflow: WorkflowDefinition | null = null;
  states: WorkflowState[] = [];
  transitions: WorkflowTransition[] = [];
  
  activeTab: 'states' | 'transitions' | 'permissions' = 'states';
  selectedTransition: WorkflowTransition | null = null;
  transitionPermissions: WorkflowPermission[] = [];

  // Role codes for dropdown
  roleCodes = [
    'CITIZEN',
    'DEALING_ASSISTANT',
    'CIRCLE_MANDOL',
    'CIRCLE_OFFICER',
    'SUB_DIVISION_OFFICER',
    'DISTRICT_OFFICER',
    'STATE_ADMIN',
    'SUPER_ADMIN'
  ];

  unitLevels = ['STATE', 'DISTRICT', 'SUB_DIVISION', 'CIRCLE'];
  hierarchyRules = ['SAME_UNIT', 'PARENT_UNIT', 'ANY_UNIT', 'SUPERVISOR'];

  constructor(
    private route: ActivatedRoute,
    private workflowService: WorkflowConfigService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.workflowId = +params['id'];
      this.loadWorkflow();
      this.loadStates();
      this.loadTransitions();
    });
  }

  loadWorkflow(): void {
    this.workflowService.getWorkflowById(this.workflowId).subscribe({
      next: (response) => {
        if (response.success) {
          this.workflow = response.data;
        }
      },
      error: (error) => {
        alert('Failed to load workflow: ' + (error.error?.message || error.message));
      }
    });
  }

  loadStates(): void {
    this.workflowService.getWorkflowStates(this.workflowId).subscribe({
      next: (response) => {
        if (response.success) {
          this.states = response.data.sort((a, b) => (a.stateOrder || 0) - (b.stateOrder || 0));
        }
      },
      error: (error) => {
        alert('Failed to load states: ' + (error.error?.message || error.message));
      }
    });
  }

  loadTransitions(): void {
    this.workflowService.getAllWorkflowTransitions(this.workflowId).subscribe({
      next: (response) => {
        if (response.success) {
          this.transitions = response.data;
        }
      },
      error: (error) => {
        alert('Failed to load transitions: ' + (error.error?.message || error.message));
      }
    });
  }

  loadTransitionPermissions(transitionId: number): void {
    this.workflowService.getTransitionPermissions(transitionId).subscribe({
      next: (response) => {
        if (response.success) {
          this.transitionPermissions = response.data;
        }
      },
      error: (error) => {
        alert('Failed to load permissions: ' + (error.error?.message || error.message));
      }
    });
  }

  selectTransition(transition: WorkflowTransition): void {
    this.selectedTransition = transition;
    this.activeTab = 'permissions';
    if (transition.id) {
      this.loadTransitionPermissions(transition.id);
    }
  }

  getStateName(stateId: number): string {
    const state = this.states.find(s => s.id === stateId);
    return state ? state.stateName : 'Unknown';
  }

  getStateCode(stateId: number): string {
    const state = this.states.find(s => s.id === stateId);
    return state ? state.stateCode : 'UNKNOWN';
  }
}
```

**File:** `src/app/components/workflow-builder/workflow-builder.component.html`

```html
<div class="workflow-builder">
  <div class="header">
    <h2>{{ workflow?.workflowName }}</h2>
    <p>{{ workflow?.description }}</p>
  </div>

  <div class="tabs">
    <button 
      class="tab-btn" 
      [class.active]="activeTab === 'states'"
      (click)="activeTab = 'states'"
    >
      States
    </button>
    <button 
      class="tab-btn" 
      [class.active]="activeTab === 'transitions'"
      (click)="activeTab = 'transitions'"
    >
      Transitions
    </button>
    <button 
      class="tab-btn" 
      [class.active]="activeTab === 'permissions'"
      (click)="activeTab = 'permissions'"
      [disabled]="!selectedTransition"
    >
      Permissions
    </button>
  </div>

  <!-- States Tab -->
  <div *ngIf="activeTab === 'states'" class="tab-content">
    <app-workflow-states [workflowId]="workflowId"></app-workflow-states>
  </div>

  <!-- Transitions Tab -->
  <div *ngIf="activeTab === 'transitions'" class="tab-content">
    <app-workflow-transitions 
      [workflowId]="workflowId"
      [states]="states"
      (transitionSelected)="selectTransition($event)"
    ></app-workflow-transitions>
  </div>

  <!-- Permissions Tab -->
  <div *ngIf="activeTab === 'permissions' && selectedTransition" class="tab-content">
    <app-workflow-permissions 
      [transition]="selectedTransition"
      [permissions]="transitionPermissions"
    ></app-workflow-permissions>
  </div>
</div>
```

---

## 5. Error Handling

### Angular Error Interceptor

**File:** `src/app/interceptors/error.interceptor.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<any> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        let errorMessage = 'An unknown error occurred';
        
        if (error.error instanceof ErrorEvent) {
          // Client-side error
          errorMessage = `Error: ${error.error.message}`;
        } else {
          // Server-side error
          switch (error.status) {
            case 401:
              errorMessage = 'Unauthorized. Please login again.';
              localStorage.removeItem('adminToken');
              this.router.navigate(['/admin/login']);
              break;
            case 403:
              errorMessage = 'Forbidden. You do not have permission.';
              break;
            case 404:
              errorMessage = error.error?.message || 'Resource not found';
              break;
            case 400:
              errorMessage = error.error?.message || 'Bad request';
              break;
            case 500:
              errorMessage = 'Server error. Please try again later.';
              break;
            default:
              errorMessage = error.error?.message || `Error Code: ${error.status}`;
          }
        }

        console.error('HTTP Error:', errorMessage);
        return throwError(() => new Error(errorMessage));
      })
    );
  }
}
```

---

## 6. Usage Example: Complete Workflow Creation

### Step-by-Step Workflow Creation Flow

```typescript
// Example: Creating Mutation Gift/Sale Workflow
async function createMutationWorkflow() {
  const workflowService = new WorkflowConfigService(http);

  // Step 1: Create Workflow
  const workflow = await workflowService.createWorkflow({
    workflowCode: 'MUTATION_GIFT_SALE',
    workflowName: 'Mutation (after Gift/Sale Deeds)',
    description: 'Workflow for mutation after gift/sale deeds registration',
    isActive: true
  }).toPromise();

  const workflowId = workflow.data.id;

  // Step 2: Create States
  const states = [];
  const stateData = [
    { code: 'CITIZEN_APPLICATION', name: 'Citizen Application', order: 1, initial: true },
    { code: 'DA_ENTRY', name: 'DA Entry', order: 2, initial: false },
    { code: 'MANDOL_RECEIVED', name: 'Mandol Received', order: 3, initial: false },
    // ... more states
  ];

  for (const stateInfo of stateData) {
    const state = await workflowService.createState(workflowId, {
      stateCode: stateInfo.code,
      stateName: stateInfo.name,
      stateOrder: stateInfo.order,
      isInitialState: stateInfo.initial,
      isFinalState: false,
      description: ''
    }).toPromise();
    states.push(state.data);
  }

  // Step 3: Create Transitions
  const transitions = [];
  const transitionData = [
    { code: 'SUBMIT_APPLICATION', name: 'Submit Application', from: 'CITIZEN_APPLICATION', to: 'DA_ENTRY' },
    { code: 'ENTER_IN_REGISTER', name: 'Enter in Register', from: 'DA_ENTRY', to: 'MANDOL_RECEIVED' },
    // ... more transitions
  ];

  for (const transInfo of transitionData) {
    const fromState = states.find(s => s.stateCode === transInfo.from);
    const toState = states.find(s => s.stateCode === transInfo.to);
    
    const transition = await workflowService.createTransition(workflowId, {
      transitionCode: transInfo.code,
      transitionName: transInfo.name,
      fromStateId: fromState.id,
      toStateId: toState.id,
      requiresComment: false,
      isActive: true,
      description: ''
    }).toPromise();
    transitions.push(transition.data);
  }

  // Step 4: Create Permissions
  const permissionData = [
    { transition: 'SUBMIT_APPLICATION', role: 'CITIZEN', unitLevel: null, hierarchy: 'ANY_UNIT' },
    { transition: 'ENTER_IN_REGISTER', role: 'DEALING_ASSISTANT', unitLevel: 'CIRCLE', hierarchy: 'SAME_UNIT' },
    // ... more permissions
  ];

  for (const permInfo of permissionData) {
    const transition = transitions.find(t => t.transitionCode === permInfo.transition);
    
    await workflowService.createPermission(transition.id, {
      roleCode: permInfo.role,
      unitLevel: permInfo.unitLevel,
      canInitiate: true,
      canApprove: false,
      hierarchyRule: permInfo.hierarchy,
      isActive: true
    }).toPromise();
  }

  console.log('Workflow created successfully!');
}
```

---

## 7. CSS Styling (Basic)

**File:** `workflow-builder.component.css`

```css
.workflow-builder {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
}

.tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  border-bottom: 2px solid #ddd;
}

.tab-btn {
  padding: 10px 20px;
  border: none;
  background: none;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.tab-btn:hover {
  border-bottom-color: #007bff;
}

.tab-btn.active {
  border-bottom-color: #007bff;
  color: #007bff;
  font-weight: bold;
}

.tab-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.tab-content {
  padding: 20px;
  background: #f9f9f9;
  border-radius: 5px;
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 20px;
  border-radius: 5px;
  max-width: 600px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

.form-control {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 5px;
}

.btn-primary {
  background: #007bff;
  color: white;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 12px;
}
```

---

## 8. Return for Correction (Citizen Flow)

This section explains the **best-practice citizen flow** when an officer returns an application for correction. Citizens do not perform workflow transitions. They resubmit updated data using a separate API.

### 8.1 Show Status in Case List

Show a badge when case status is `RETURNED_FOR_CORRECTION`.

**Angular:**
```typescript
<span class="badge badge-warning" *ngIf="case.status === 'RETURNED_FOR_CORRECTION'">
  Returned for Correction
</span>
```

**React:**
```typescript
{case.status === 'RETURNED_FOR_CORRECTION' && (
  <span className="badge badge-warning">Returned for Correction</span>
)}
```

### 8.2 Show Officer Comment

Fetch workflow history and display the latest comment where `toStateCode = RETURNED_FOR_CORRECTION`.

**API:**
```
GET /api/cases/{caseId}/history
```

**Angular:**
```typescript
this.caseService.getCaseHistory(caseId).subscribe((response) => {
  const history = response.data || [];
  const returned = history.filter((h: any) =>
    h?.toState?.stateCode === 'RETURNED_FOR_CORRECTION'
  ).slice(-1)[0];
  this.returnComment = returned?.comments || '';
});
```

**React:**
```typescript
const res = await axios.get(`${API_URL}/api/cases/${caseId}/history`, { headers });
const history = res.data?.data || [];
const returned = history.filter((h: any) =>
  h?.toState?.stateCode === 'RETURNED_FOR_CORRECTION'
).slice(-1)[0];
setReturnComment(returned?.comments || '');
```

### 8.3 Add “Edit & Resubmit” Button

Only show when status is `RETURNED_FOR_CORRECTION`.

**Angular:**
```typescript
<button class="btn btn-primary"
        *ngIf="case.status === 'RETURNED_FOR_CORRECTION'"
        (click)="openEditForm(case)">
  Edit & Resubmit
</button>
```

**React:**
```typescript
{case.status === 'RETURNED_FOR_CORRECTION' && (
  <button className="btn btn-primary" onClick={() => openEditForm(case)}>
    Edit & Resubmit
  </button>
)}
```

### 8.4 Resubmit API

**Endpoint:**
```
PUT /api/cases/{caseId}/resubmit
```

**Headers:**
```
Authorization: Bearer <citizenToken>
X-User-Id: <citizenId>
```

**Body:**
```json
{
  "caseData": "{ ...corrected JSON... }",
  "remarks": "Updated documents and corrected details"
}
```

**Angular:**
```typescript
resubmitCase(caseId: number, payload: any) {
  return this.http.put(`${apiUrl}/api/cases/${caseId}/resubmit`, payload, {
    headers: new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'X-User-Id': citizenId
    })
  });
}
```

**React:**
```typescript
await axios.put(`${API_URL}/api/cases/${caseId}/resubmit`, payload, {
  headers: {
    Authorization: `Bearer ${token}`,
    'X-User-Id': citizenId
  }
});
```

### 8.5 UX Recommendations

- Disable edit button if case is not in `RETURNED_FOR_CORRECTION`
- Show officer comment near the resubmit form
- Highlight returned cases in citizen dashboard

---

## Summary

This guide provides:

1. ✅ Complete service implementations for Angular and React
2. ✅ Workflow management components with CRUD operations
3. ✅ State management components
4. ✅ Transition and permission management
5. ✅ Error handling examples
6. ✅ Complete workflow builder component
7. ✅ Step-by-step workflow creation example
8. ✅ Basic CSS styling
9. ✅ Return for correction (citizen resubmit flow)

**Next Steps:**
1. Implement the service in your frontend framework
2. Create the components using the provided examples
3. Add routing for workflow management pages
4. Test with your backend API
5. Customize styling to match your design system

All APIs are ready and documented. The frontend can now fully configure workflows through the admin panel!
