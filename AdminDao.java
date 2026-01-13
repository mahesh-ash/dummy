import React, { useRef, useState, useMemo, useEffect, useCallback } from 'react';
import { AgGridReact } from "ag-grid-react";
import { Pencil, Trash2, Plus, Download, Upload, History, RotateCcw, GitCompare, RefreshCcw, X } from 'lucide-react';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetFooter } from '@/components/ui/sheet';
import { Button } from '@/components/ui/button';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@/components/ui/select';
import { useForm } from 'react-hook-form';
import { Input } from '@/components/ui/input';
import { toast } from 'sonner';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { useSignals } from '@preact/signals-react/runtime';
import { Textarea } from '@/components/ui/textarea';
import {
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { 
  ModuleRegistry,
  ColumnsToolPanelModule, 
  SetFilterModule, 
  ColumnMenuModule, 
  ContextMenuModule 
} from 'ag-grid-enterprise';

ModuleRegistry.registerModules([
  ColumnsToolPanelModule,
  SetFilterModule,
  ColumnMenuModule,
  ContextMenuModule
]);

const defaultColDef = {
	sortable: true,
	editable: false,
	flex: 1,
	filter: 'agTextColumnFilter',
	floatingFilter: true,
	cellStyle: {
		whiteSpace: 'nowrap',
		overflow: 'hidden',
		textOverflow: 'ellipsis',
	},
};

// Field Configuration with insertion order
const FIELD_CONFIG = [
	{
		name: 'constant_name',
		label: 'Constants Name',
		placeholder: 'Enter constant name',
		type: 'text',
		mandatory: true,
		disabled: false,
		hidden: false,
		readOnly: false,
		pattern: null,
		patternMessage: '',
		defaultValue: '',
		className: '',
		maxLength: 100,
		order: 1,
	},
	{
		name: 'constant_values',
		label: 'Constant Values (JSON)',
		placeholder: 'Enter JSON values (e.g., {"key": "value"})',
		type: 'textarea',
		mandatory: true,
		disabled: false,
		hidden: false,
		readOnly: false,
		pattern: null,
		patternMessage: 'Invalid JSON format',
		defaultValue: '',
		className: '',
		order: 2,
		isJSON: true, 
	},
	{
		name: 'description',
		label: 'Description',
		placeholder: 'Enter description',
		type: 'textarea',
		mandatory: false,
		disabled: false,
		hidden: false,
		readOnly: false,
		pattern: null,
		patternMessage: '',
		defaultValue: '',
		className: '',
		order: 3,
	},
	{
		name: 'created_version',
		label: 'Created Version',
		placeholder: 'e.g., v1.0.0',
		type: 'text',
		mandatory: true,
		disabled: false,
		hidden: false,
		readOnly: false,
		pattern: /^v\d+\.\d+\.\d+$/,
		patternMessage: 'Version format should be v1.0.0',
		defaultValue: '',
		className: '',
		maxLength: 20,
		order: 4,
	},
	{
		name: 'updated_version',
		label: 'Updated Version',
		placeholder: 'e.g., v1.0.1',
		type: 'text',
		mandatory: false,
		disabled: false,
		hidden: false,
		readOnly: false,
		pattern: /^v\d+\.\d+\.\d+$/,
		patternMessage: 'Version format should be v1.0.0',
		defaultValue: '',
		className: '',
		maxLength: 20,
		order: 5,
	},
	// {
	// 	name: 'updated_version',
	// 	label: 'Updated Version',
	// 	placeholder: 'e.g., v1.0.1',
	// 	type: 'text',
	// 	mandatory: false,
	// 	disabled: false,
	// 	hidden: false,
	// 	readOnly: false,
	// 	pattern: /^v\d+\.\d+\.\d+$/,
	// 	patternMessage: 'Version format should be v1.0.0',
	// 	defaultValue: '',
	// 	className: '',
	// 	maxLength: 20,
	// 	order: 5,
	// },
	{
		name: 'status',
		label: 'Status',
		placeholder: 'Select status',
		type: 'select',
		mandatory: true,
		disabled: false,
		hidden: false,
		readOnly: false,
		pattern: null,
		patternMessage: '',
		defaultValue: 'Active',
		className: '',
		options: [
			{ label: 'Active', value: 'Active' },
			{ label: 'Inactive', value: 'Inactive' },
		],
		order: 6,
	},
];

function StatusCellRenderer(props) {
	const isActive = props.value === 'Active';
	
	return (
		<div className="flex items-center justify-center h-full">
			<span
				className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold ${
					isActive
						? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
						: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
				}`}
			>
				{props.value}
			</span>
		</div>
	);
}

function DateCellRenderer(props) {
	if (!props.value) return <div>-</div>;
	const date = new Date(props.value);
	return (
		<div title={props.value}>
			{date.toLocaleString('en-US', {
				year: 'numeric',
				month: 'short',
				day: 'numeric',
				hour: '2-digit',
				minute: '2-digit',
			})}
		</div>
	);
}

function DefaultCellRenderer(props) {
	const col = props.colDef;
	return (
		<div className={col.className} style={col.cellStyle} title={props.value}>
			{props.value == 'null' || props.value == null || !props.value ? '-' : props.value}
		</div>
	);
}

export default function Constants(props) {
	useSignals();
	const gridRef = useRef();

	// States
	const [tableData, setTableData] = useState([]);
	const [showDeleteDialog, setShowDeleteDialog] = useState(false);
	const [rowToDelete, setRowToDelete] = useState(null);
	const [showCompareDialog, setShowCompareDialog] = useState(false);
	const [compareData, setCompareData] = useState({ original: null, modified: null });
	const [excelLoading, setExcelLoading] = useState(false);
	const [lastRefresh, setLastRefresh] = useState(null);
	const [editingRow, setEditingRow] = useState(null);
	const [showFormSheet, setShowFormSheet] = useState(false);

	// Sort fields by order
	const sortedFields = useMemo(() => {
		return [...FIELD_CONFIG].sort((a, b) => a.order - b.order);
	}, []);

	// Default values
	const defaultValues = useMemo(() => {
		return FIELD_CONFIG.reduce((acc, field) => {
			acc[field.name] = field.defaultValue;
			return acc;
		}, {});
	}, []);

	const form = useForm({
		mode: 'onChange',
		defaultValues,
	});

	// Validate JSON
	const validateJSON = (value) => {
		if (!value) return true;
		try {
			JSON.parse(value);
			return true;
		} catch (e) {
			return 'Invalid JSON format';
		}
	};

	// Column Definitions
	const columnDefs = useMemo(
		() => [
			{
				headerName: 'Constants Name',
				field: 'constant_name',
				sortable: true,
				filter: 'agTextColumnFilter',
				floatingFilter: true,
				flex: 1,
				minWidth: 150,
				cellClass: 'font-medium',
			},
			{
				headerName: 'Constant Values',
				field: 'constant_values',
				sortable: false,
				filter: 'agTextColumnFilter',
				floatingFilter: true,
				flex: 2,
				minWidth: 200,
				cellRenderer: (params) => (
					<div className="truncate" title={params.value}>
						{params.value}
					</div>
				),
			},
			{
				headerName: 'Description',
				field: 'description',
				sortable: false,
				filter: 'agTextColumnFilter',
				floatingFilter: true,
				flex: 1.5,
				minWidth: 150,
				cellRenderer: DefaultCellRenderer,
			},
			{
				headerName: 'Created Version',
				field: 'created_version',
				sortable: true,
				filter: 'agTextColumnFilter',
				floatingFilter: true,
				width: 140,
				cellClass: 'text-center',
			},
			{
				headerName: 'Updated Version',
				field: 'updated_version',
				sortable: true,
				filter: 'agTextColumnFilter',
				floatingFilter: true,
				width: 140,
				cellClass: 'text-center',
				cellRenderer: DefaultCellRenderer,
			},
			{
				headerName: 'Status',
				field: 'status',
				sortable: true,
				filter: 'agSetColumnFilter',
				floatingFilter: true,
				width: 120,
				cellRenderer: StatusCellRenderer,
			},
			{
				headerName: 'Last Modified',
				field: 'last_modified',
				sortable: true,
				filter: 'agDateColumnFilter',
				floatingFilter: true,
				width: 180,
				cellRenderer: DateCellRenderer,
			},
			{
				headerName: 'Action',
				field: 'action',
				width: 120,
				pinned: 'right',
				cellRenderer: ActionCellRenderer,
				sortable: false,
				filter: false,
				floatingFilter: false,
			},
		],
		[]
	);

	const sideBar = useMemo(
		() => ({
			toolPanels: [
				{
					id: 'columns',
					labelDefault: 'Columns',
					labelKey: 'columns',
					iconKey: 'columns',
					toolPanel: 'agColumnsToolPanel',
					toolPanelParams: {
						suppressRowGroups: true,
						suppressValues: true,
						suppressPivotMode: true,
						suppressColumnFilter: true,
						suppressColumnSelectAll: true,
						suppressColumnExpandAll: true,
					},
				},
			],
			defaultToolPanel: '',
		}),
		[]
	);

	function ActionCellRenderer(params) {
		const row = params.data;
		return (
			<div className="flex items-center justify-center h-full">
				<DropdownMenu>
					<DropdownMenuTrigger asChild>
						<Button variant="ghost" size="sm" className="h-8 px-2 text-xs font-medium">
							Action
						</Button>
					</DropdownMenuTrigger>
					<DropdownMenuContent align="end" className="w-32">
						<DropdownMenuItem onClick={() => handleEdit(row)} className="cursor-pointer">
							<Pencil className="mr-2 h-4 w-4" />
							Edit
						</DropdownMenuItem>
						<DropdownMenuItem onClick={() => handleDelete(row)} className="cursor-pointer text-red-600">
							<Trash2 className="mr-2 h-4 w-4" />
							Delete
						</DropdownMenuItem>
						<DropdownMenuItem onClick={() => handleHistory(row)} className="cursor-pointer">
							<History className="mr-2 h-4 w-4" />
							History
						</DropdownMenuItem>
					</DropdownMenuContent>
				</DropdownMenu>
			</div>
		);
	}

	// Handle Add New
	const handleAddNew = () => {
		setEditingRow(null);
		form.reset(defaultValues);
		setShowFormSheet(true);
	};

	// Handle Edit
	const handleEdit = (data) => {
		setEditingRow(data);
		form.reset(data);
		setShowFormSheet(true);
		toast.info('Editing constant');
	};

	// Handle Delete
	const handleDelete = (row) => {
		setRowToDelete(row);
		setShowDeleteDialog(true);
	};

	const handleDeleteConfirm = () => {
		if (rowToDelete) {
			setTableData((prev) => prev.filter((row) => row.id !== rowToDelete.id));
			toast.success('Constant deleted successfully');
			setShowDeleteDialog(false);
			setRowToDelete(null);
		}
	};

	const handleDeleteCancel = () => {
		setShowDeleteDialog(false);
		setRowToDelete(null);
	};

	// Handle History
	const handleHistory = (data) => {
		toast.info(`Viewing history for: ${data.constant_name}`);
	};

	// Handle Add/Update Submit
	const handleFormSubmit = (data) => {
		try {
			// Validate JSON
			if (data.constant_values) {
				JSON.parse(data.constant_values);
			}

			const timestamp = new Date().toISOString();

			if (editingRow) {
				// Update existing row
				setTableData((prev) =>
					prev.map((row) =>
						row.id === editingRow.id
							? {
									...data,
									id: row.id,
									last_modified: timestamp,
									created_at: row.created_at,
							  }
							: row
					)
				);
				toast.success('Constant updated successfully');
				setEditingRow(null);
			} else {
				// Add new row
				const newRow = {
					...data,
					id: Date.now(),
					created_at: timestamp,
					last_modified: timestamp,
				};
				setTableData((prev) => [...prev, newRow]);
				toast.success('Constant added successfully');
			}

			form.reset(defaultValues);
			setShowFormSheet(false);
		} catch (error) {
			toast.error('Invalid JSON format in Constant Values');
		}
	};

	// Handle Reset
	const handleReset = () => {
		form.reset(editingRow || defaultValues);
		toast.info('Form reset successfully');
	};

	// Handle Compare JSON
	const handleCompareJSON = () => {
		const currentData = form.getValues();
		if (editingRow) {
			setCompareData({
				original: editingRow,
				modified: currentData,
			});
			setShowCompareDialog(true);
		} else {
			toast.warning('Please edit a constant to compare');
		}
	};

	// Handle Refresh
	const handleRefresh = () => {
		setLastRefresh(new Date());
		gridRef.current?.api?.refreshCells({ force: true });
		toast.success('Table refreshed');
	};

	// Export to JSON
	const handleExport = () => {
		try {
			setExcelLoading(true);
			const dataStr = JSON.stringify(tableData, null, 2);
			const dataBlob = new Blob([dataStr], { type: 'application/json' });
			const url = URL.createObjectURL(dataBlob);
			const link = document.createElement('a');
			link.href = url;
			link.download = `constants_${new Date().toISOString().slice(0, 10)}.json`;
			document.body.appendChild(link);
			link.click();
			link.remove();
			URL.revokeObjectURL(url);
			toast.success('Constants exported successfully');
		} catch (error) {
			toast.error('Export failed');
		} finally {
			setExcelLoading(false);
		}
	};

	// Import from JSON
	const handleImport = (event) => {
		const file = event.target.files?.[0];
		if (!file) return;

		const reader = new FileReader();
		reader.onload = (e) => {
			try {
				const imported = JSON.parse(e.target.result);
				if (Array.isArray(imported)) {
					setTableData(imported);
					toast.success('Constants imported successfully');
				} else {
					toast.error('Invalid JSON format');
				}
			} catch (error) {
				toast.error('Failed to parse JSON file');
			}
		};
		reader.readAsText(file);
		event.target.value = '';
	};

	// Render Form Field
	const renderFormField = (config) => {
		if (config.hidden) return null;

		const rules = {
			required: config.mandatory ? `${config.label} is required` : false,
			pattern: config.pattern
				? {
						value: config.pattern,
						message: config.patternMessage || `Invalid ${config.label} format`,
				  }
				: undefined,
			validate: config.name === 'constant_values' ? validateJSON : undefined,
		};

		switch (config.type) {
			case 'textarea':
				return (
					<FormField
						key={config.name}
						control={form.control}
						name={config.name}
						rules={rules}
						render={({ field }) => (
							<FormItem className="w-full">
								<FormLabel className={`gap-1 font-medium text-sm ${config.className}`}>
									{config.label}
									{config.mandatory && <span className="text-red-500 ml-1">*</span>}
								</FormLabel>
								<div>
									<Textarea
										{...field}
										disabled={config.disabled || config.readOnly}
										className={`shadow-none ${config.isJSON ? 'h-[200px]' : 'h-[120px]'} resize-none disabled:bg-gray-100 dark:disabled:bg-gray-800 disabled:cursor-not-allowed disabled:opacity-100 ${config.className}`}
										placeholder={config.placeholder}
										required={config.mandatory}
									/>
									<div className="min-h-[20px]">
										<FormMessage />
									</div>
								</div>
							</FormItem>
						)}
					/>
				);

			case 'select':
				return (
					<FormField
						key={config.name}
						control={form.control}
						name={config.name}
						rules={rules}
						render={({ field }) => (
							<FormItem className="w-full">
								<FormLabel className={`gap-1 font-medium text-sm ${config.className}`}>
									{config.label}
									{config.mandatory && <span className="text-red-500 ml-1">*</span>}
								</FormLabel>
								<div>
									<Select value={field.value} onValueChange={field.onChange} disabled={config.disabled} required={config.mandatory}>
										<SelectTrigger
											className={`flex h-[50px] w-full rounded-md border border-input bg-background py-2 px-4 ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-0 text-sm disabled:opacity-50 disabled:bg-gray-100 dark:disabled:bg-gray-800 disabled:cursor-not-allowed ${config.className}`}
										>
											<SelectValue placeholder={config.placeholder || `Select ${config.label}`} />
										</SelectTrigger>
										<SelectContent>
											{config.options.map((option) => (
												<SelectItem key={option.value} value={option.value}>
													{option.label}
												</SelectItem>
											))}
										</SelectContent>
									</Select>
									<div className="min-h-[20px]">
										<FormMessage />
									</div>
								</div>
							</FormItem>
						)}
					/>
				);

			case 'text':
			default:
				return (
					<FormField
						key={config.name}
						control={form.control}
						name={config.name}
						rules={rules}
						render={({ field }) => (
							<FormItem className="w-full">
								<FormLabel className={`gap-1 font-medium text-sm ${config.className}`}>
									{config.label}
									{config.mandatory && <span className="text-red-500 ml-1">*</span>}
								</FormLabel>
								<div>
									<Input
										{...field}
										type="text"
										disabled={config.disabled || config.readOnly}
										maxLength={config.maxLength}
										className={`flex h-[50px] w-full rounded-md border border-input bg-background py-2 px-4 ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-0 text-sm disabled:opacity-50 disabled:bg-gray-100 dark:disabled:bg-gray-800 disabled:cursor-not-allowed ${config.className}`}
										placeholder={config.placeholder}
										required={config.mandatory}
									/>
									<div className="min-h-[20px]">
										<FormMessage />
									</div>
								</div>
							</FormItem>
						)}
					/>
				);
		}
	};

	return (
		<div className="w-full h-screen flex flex-col overflow-hidden">
			{/* Main Content Area */}
			<div className="flex-1 overflow-y-auto overflow-x-hidden">
				<div className="p-4 sm:p-6 space-y-4">
					{/* Header with Title and Action Buttons */}
					<div className="flex flex-col sm:flex-row justify-between gap-4 items-start sm:items-center">
						{/* Left Side - Title */}
						<h2 className="text-xl font-semibold dark:text-white">Constants Management</h2>

						{/* Right Side - Action Buttons */}
						<div className="flex flex-wrap gap-2 items-center">
							<Button onClick={handleAddNew} className="gap-2 h-[40px] bg-blue-600 hover:bg-blue-700 text-white">
								<Plus size={18} />
								<span className="hidden sm:inline">Add New</span>
							</Button>

							<TooltipProvider>
								<Tooltip>
									<TooltipTrigger asChild>
										<Button onClick={handleRefresh} className="h-[40px]" variant="outline" size="sm">
											<RefreshCcw className="stroke-[#9ca3af]" size={18} />
										</Button>
									</TooltipTrigger>
									<TooltipContent>
										Last Refreshed: {lastRefresh ? new Date(lastRefresh).toLocaleString() : 'Never'}
									</TooltipContent>
								</Tooltip>
							</TooltipProvider>

							<Button onClick={handleExport} variant="outline" size="sm" className="gap-2 h-[40px]" disabled={tableData.length === 0 || excelLoading}>
								<Download size={18} />
								<span className="hidden sm:inline">Export</span>
							</Button>

							<label>
								<Button variant="outline" size="sm" className="gap-2 h-[40px]" asChild>
									<span>
										<Upload size={18} />
										<span className="hidden sm:inline">Import</span>
									</span>
								</Button>
								<input type="file" accept=".json" onChange={handleImport} className="hidden" />
							</label>
						</div>
					</div>

					{/* AG Grid Table */}
					<div className="bg-white dark:bg-gray-800 border dark:border-gray-700 rounded-lg overflow-hidden">
						<div className="w-full" style={{ height: '600px', overflowX: 'auto', overflowY: 'auto' }}>
							<div className="w-full h-full min-w-[800px]">
								<AgGridReact
									ref={gridRef}
									rowData={tableData}
									columnDefs={columnDefs}
									defaultColDef={defaultColDef}
									modules={[ColumnsToolPanelModule, SetFilterModule]}
									rowHeight={40}
									cellSelection
									headerHeight={40}
									pagination={true}
									paginationPageSize={20}
									domLayout="normal"
									sideBar={sideBar}
									suppressMovableColumns
									noRowsOverlayComponent={() => <span>Record not found.</span>}
									className="ag-theme-quartz w-full h-full"
								/>
							</div>
						</div>
					</div>
				</div>
			</div>

			{/* Form Sheet */}
			<Sheet open={showFormSheet} onOpenChange={setShowFormSheet}>
				<SheetContent side="right" className="w-full sm:max-w-2xl overflow-y-auto">
					<SheetHeader className="mb-6">
						<SheetTitle className="text-xl">
							{editingRow ? 'Edit Constant' : 'Add New Constant'}
						</SheetTitle>
					</SheetHeader>

					<Form {...form}>
						<div autoComplete="off" className="space-y-6">
							{/* Form Fields */}
							<div className="space-y-6">
								{sortedFields.map((field) => renderFormField(field))}
							</div>

							{/* Action Buttons */}
							<SheetFooter className="flex flex-col sm:flex-row gap-2 pt-6 border-t dark:border-gray-700">
								<Button 
									type="button" 
									size="sm" 
									onClick={handleReset} 
									variant="outline"
									className="gap-2 h-[44px]"
								>
									<RotateCcw size={16} />
									Reset
								</Button>
								
								{editingRow && (
									<Button 
										type="button" 
										size="sm" 
										onClick={handleCompareJSON} 
										className="gap-2 h-[44px] bg-cyan-600 hover:bg-cyan-700 text-white"
									>
										<GitCompare size={16} />
										Compare
									</Button>
								)}

								<Button
									className={`h-[44px] flex-1 bg-green-600 hover:bg-green-700 text-white ${!form.formState.isValid ? 'opacity-50 cursor-not-allowed' : ''}`}
									type="button"
									onClick={form.handleSubmit(handleFormSubmit)}
									disabled={!form.formState.isValid}
								>
									{editingRow ? 'Update' : 'Save'}
								</Button>
							</SheetFooter>
						</div>
					</Form>
				</SheetContent>
			</Sheet>

			{/* Delete Dialog */}
			<Dialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
				<DialogContent>
					<DialogHeader>
						<DialogTitle>Delete Constant</DialogTitle>
					</DialogHeader>
					<div>Are you sure you want to delete this constant? This action cannot be undone.</div>
					<DialogFooter className="flex justify-end gap-2">
						<Button variant="outline" onClick={handleDeleteCancel}>
							Cancel
						</Button>
						<Button variant="destructive" onClick={handleDeleteConfirm}>
							Delete
						</Button>
					</DialogFooter>
				</DialogContent>
			</Dialog>

			{/* Compare Dialog */}
			{showCompareDialog && (
				<Dialog open={showCompareDialog} onOpenChange={setShowCompareDialog}>
					<DialogContent className="max-w-4xl max-h-[90vh] overflow-auto">
						<DialogHeader>
							<DialogTitle>Compare JSON</DialogTitle>
						</DialogHeader>
						<div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
							<div>
								<h3 className="font-semibold mb-2 text-sm dark:text-white">Original</h3>
								<pre className="bg-gray-100 dark:bg-gray-900 p-3 rounded text-xs overflow-auto max-h-96">
									{JSON.stringify(compareData.original, null, 2)}
								</pre>
							</div>
							<div>
								<h3 className="font-semibold mb-2 text-sm dark:text-white">Modified</h3>
								<pre className="bg-gray-100 dark:bg-gray-900 p-3 rounded text-xs overflow-auto max-h-96">
									{JSON.stringify(compareData.modified, null, 2)}
								</pre>
							</div>
						</div>
						<DialogFooter>
							<Button variant="outline" onClick={() => setShowCompareDialog(false)}>
								Close
							</Button>
						</DialogFooter>
					</DialogContent>
				</Dialog>
			)}
		</div>
	);
}
