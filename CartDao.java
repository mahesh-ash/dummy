import React, { useRef, useState, useMemo, useEffect, useCallback } from 'react';
import { AsyncTypeahead } from 'react-bootstrap-typeahead';
import { AgGridReact } from '@ag-grid-community/react';
import { ColumnsToolPanelModule } from '@ag-grid-enterprise/column-tool-panel';
import { SetFilterModule } from '@ag-grid-enterprise/set-filter';
import { MenuModule } from '@ag-grid-enterprise/menu';
import { ClientSideRowModelModule } from '@ag-grid-community/client-side-row-model';
import { Eye, Pencil, Trash2, Plus, Download, CircleDot, CheckCircle2, Clock, RefreshCcw, RefreshCcwIcon } from 'lucide-react';
import { Menu, MenuItem } from 'react-bootstrap-typeahead';
import { ChevronDownCircle } from 'lucide-react';
import httpClient from '@/common/helpers/httpClient';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetFooter } from '@/components/ui/sheet';
import { Button } from '@/components/ui/button';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Calendar } from '@/components/ui/calendar';
import { CalendarIcon } from 'lucide-react';
import { Form, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@/components/ui/select';
import { useForm } from 'react-hook-form';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { toast } from 'sonner';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { IS_DARK } from '@/store/signals';
import { useSignals } from '@preact/signals-react/runtime';
import { DateRangePicker } from '../common/DateRangePicker';
import { useMutation } from 'react-query';
import { saveinvoicemutations } from '@/utils/service';
import { createFormData } from '@/utils/common';
import z from 'zod';

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

const columns = [
	{
		headerName: 'Account Name',
		field: 'accountName',
		filterParams: {
			filterPlaceholder: (params) => {
				return 'Search Account Name';
			},
		},
		lockVisible: true,
	},
	{ headerName: 'Account ID', field: 'accountId', cellClass: 'justify-center whitespace-nowrap overflow-hidden text-ellipsis' },
	{ headerName: 'Invoice Date', field: 'date', headerClass: 'justify-center', filter: false, cellClass: 'justify-center whitespace-nowrap overflow-hidden text-ellipsis' },
	{ headerName: 'Commission', field: 'commission', cellClass: 'justify-center whitespace-nowrap overflow-hidden text-ellipsis' },
	{ headerName: 'Status', field: 'status', cellRenderer: 'StatusCellRenderer', cellClass: 'justify-center whitespace-nowrap overflow-hidden text-ellipsis' },
	{ headerName: 'Invoice', field: 'invoice', cellClass: 'justify-center whitespace-nowrap overflow-hidden text-ellipsis' },
	{ headerName: 'Notes', field: 'notes' },
	{
		headerName: 'Action',
		field: 'action',
		filter: false,
		sortable: false,
		editable: false,
		menuTabs: [],
		cellRenderer: 'ActionCellRenderer',
		pinned: 'right',
		minWidth: 120,
		maxWidth: 140,
		filter: false,
		sortable: false,
		lockVisible: true,
		suppressHeaderMenuButton: true,
		cellStyle: { display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' },
	},
];

function DefaultCellRenderer(props) {
	const col = props.colDef;
	if (col.field === 'invoice') {
		return (
			<div className={col.className + ' relative flex items-center'} style={col.cellStyle} title={props.value}>
				<span>{props.value}</span>
				<button
					type="button"
					className="ml-2 text-blue-500 hover:text-blue-700 transition-colors"
					style={{ background: 'none', border: 'none', cursor: 'pointer' }}
					title="Download Invoice"
					onClick={() => {
						toast.success('Download Completed');
					}}
				>
					<Download size={18} />
				</button>
			</div>
		);
	}
	return (
		<div className={col.className} style={col.cellStyle} title={props.value}>
			{props.value}
		</div>
	);
}

function StatusCellRenderer(props) {
	const status = props.value;
	const isDark = IS_DARK.value;
	let color = isDark ? 'bg-gray-800 text-gray-200' : 'bg-gray-200 text-gray-800';
	let icon = <CircleDot size={16} className={isDark ? 'mr-1 text-gray-400' : 'mr-1'} />;
	if (status === 'Open') {
		color = isDark ? 'bg-yellow-900 text-yellow-200' : 'bg-yellow-100 text-yellow-800';
		icon = <Clock size={16} className={isDark ? 'mr-1 text-yellow-300' : 'mr-1 text-yellow-600'} />;
	}
	if (status === 'Paid') {
		color = isDark ? 'bg-green-900 text-green-200' : 'bg-green-100 text-green-800';
		icon = <CheckCircle2 size={16} className={isDark ? 'mr-1 text-green-300' : 'mr-1 text-green-600'} />;
	}
	if (status === 'Pending') {
		color = isDark ? 'bg-blue-900 text-blue-200' : 'bg-blue-100 text-blue-800';
		icon = <CircleDot size={16} className={isDark ? 'mr-1 text-blue-300' : 'mr-1 text-blue-600'} />;
	}
	return (
		<span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold gap-1 ${color}`} style={{ minWidth: 80 }}>
			{icon}
			{status}
		</span>
	);
}

export default function ManageInvoiceTable(props) {
	useSignals();
	const saveinvoicemutation = useMutation({
		mutationFn: saveinvoicemutations,
	});

	// Loading states
	const [isLoadingData, setIsLoadingData] = useState(false);
	const [isSaving, setIsSaving] = useState(false);

	// Typeahead for Account Name
	const [accountOptions, setAccountOptions] = useState([]);
	const [accountQuery, setAccountQuery] = useState('');
	const [selectedAccount, setSelectedAccount] = useState([]);

	// Grid and sheet states
	const gridRef = useRef();
	const [rowData, setRowData] = useState([]);
	const { sheetOpen, setSheetOpen } = props;
	const [showUnsavedDialog, setShowUnsavedDialog] = useState(false);
	const [editingRow, setEditingRow] = useState(null);
	const [showDeleteDialog, setShowDeleteDialog] = useState(false);
	const [rowToDelete, setRowToDelete] = useState(null);

	// Coverage states
	const masterCoverageOptions = [
		{ name: 'John Doe', role: 'Manager', coverageStart: '2025/01/01', coverageEnd: '2025/12/31' },
		{ name: 'Jane Smith', role: 'Analyst', coverageStart: '2025/03/01', coverageEnd: '2025/09/30' },
		{ name: 'Acme Corp', role: 'Partner', coverageStart: '2025/05/01', coverageEnd: '2025/11/30' },
	];
	const [search, setSearch] = useState('');
	const [showDropdown, setShowDropdown] = useState(false);
	const [selectedCoverage, setSelectedCoverage] = useState(null);
	const [percentage, setPercentage] = useState('');
	const [coverageRows, setCoverageRows] = useState([]);
	const [coverageError, setCoverageError] = useState('');
	const [percentageInputError, setPercentageInputError] = useState('');
	const [selectedEmployee, setSelectedEmployee] = useState([]);

	const filteredOptions = masterCoverageOptions.filter((opt) => opt.name.toLowerCase().includes(search.toLowerCase()));

	useEffect(() => {
		if (percentageInputError != '') {
			toast.error(percentageInputError);
		}
	}, [percentageInputError]);

	const [employeeQuery, setEmployeeQuery] = useState('');
	const [employeeOptions, setEmployeeOptions] = useState([]);
	const [isEmployeeLoading, setIsEmployeeLoading] = useState(false);
	const [selectedEmployeeCoverage, setSelectedEmployeeCoverage] = useState(null);
	const [isLoading, setIsLoading] = useState(false);

	async function handleEmployeeSearch(query) {
		setEmployeeQuery(query);
		setIsEmployeeLoading(true);
		console.log(selectedAccount);
		const employeeId = selectedAccount[0].account_id;

		if (!employeeId) {
			console.warn('No employee selected yet.');
			setIsEmployeeLoading(false);
			return;
		}

		try {
			httpClient.get(`Search/GetSearchText?searchText=${employeeId}-${encodeURIComponent(query)}&type=empSearchByAccIDTxtQuery`).then((data) => {
				setEmployeeOptions(data.data.data);
				setIsEmployeeLoading(false);
			});
		} catch (error) {
			console.error('Error searching Employees:', error);
			setIsEmployeeLoading(false);
		}
	}

	useEffect(() => {
		if (saveinvoicemutation.data && saveinvoicemutation.data.status === 1) {
			setSheetOpen(false);
			form.reset(defaultValues);
		} else if (saveinvoicemutation.data && saveinvoicemutation.data.status === 0) {
			setSheetOpen(false);
			setDisableSaveBtn(false);
			form.reset(defaultValues);
		}
	}, [saveinvoicemutation.isSuccess]);

	async function handleAccountSearch(query) {
		setAccountQuery(query);
		setIsLoading(true);
		try {
			httpClient.get(`Search/GetSearchText?searchText=${encodeURIComponent(query)}&type=accountQuery`).then((data) => {
				setAccountOptions(data.data.data);
				setIsLoading(false);
			});
		} catch (error) {
			console.error('Error searching accounts:', error);
			setIsLoading(false);
		}
	}

	function dateRangeHandler(fromDate, toDate) {
		// Implement date range filtering
		console.log('Date range:', fromDate, toDate);
	}

	function dateRangeCancelHandler() {
		// Reset date range filter
	}

	async function handleAccountChange(selected) {
		setSelectedAccount(selected);

		if (selected.length > 0) {
			const accountId = selected[0].account_id;
			form.setValue('account_id', accountId, { shouldValidate: true });

			try {
				const response = await httpClient.get(`Search/GetSearchText?searchText=${accountId}&type=employeeByAccIDQuery`);

				if (response.data && response.data.data && response.data.data.length > 0) {
					const employees = response.data.data;
					setCoverageRows(employees);

					const splitsArray = employees.map((emp) => ({
						emp_id: emp.emp_id.toString(),
						split: '',
					}));
					form.setValue('emp_splits', JSON.stringify(splitsArray), { shouldValidate: true });
				} else {
					setCoverageRows([]);
					const emptyFormat = [{ emp_id: '', split: '' }];
					form.setValue('emp_splits', JSON.stringify(emptyFormat), { shouldValidate: true });
				}
			} catch (error) {
				setCoverageRows([]);
				form.setValue('emp_splits', JSON.stringify([{ emp_id: '', split: '' }]));
			}
		} else {
			form.setValue('account_id', '');
			setCoverageRows([]);
			form.setValue('emp_splits', JSON.stringify([{ emp_id: '', split: '' }]));
		}
	}

	function handleEmployeeChange(selected) {
		setSelectedEmployee(selected);
	}

	function renderAccountMenu(results, menuProps) {
    if (accountQuery.length === 0 && accountOptions.length === 0) {
        return null;
    }

    // DESTRICTURING: Remove the props React is complaining about
    const { 
        paginationText, 
        newSelectionPrefix, 
        renderMenuItemChildren, 
        ...validMenuProps 
    } = menuProps;

    const items = results.map((result, index) => {
        if (result.paginationOption) {
            return (
                <MenuItem className="default-menu-item rounded !p-0 justify-center gap-1 text-xm" href="javascript:void();" key={index} option={result}>
                    <div>Show More</div>
                    <ChevronDownCircle className="size-4 stroke-[#aaa]" />
                </MenuItem>
            );
        } else {
            return (
                <MenuItem className="default-menu-item" href="javascript:void();" key={index} option={result}>
                    <div>{result.account_name}</div>
                </MenuItem>
            );
        }
    });

    return (
        // Use validMenuProps instead of the original menuProps
        <Menu className="default-dropdown" {...validMenuProps}>
            {items}
        </Menu>
    );
}


	function renderEmployeeMenu(results, menuProps) {
		if (employeeQuery.length === 0 && employeeOptions.length === 0) {
			return null;
		}

		const { newSelectionPrefix, ...validMenuProps } = menuProps;
		const items = results.map((result, index) => {
			if (result.paginationOption) {
				return (
					<MenuItem className="default-menu-item rounded !p-0 justify-center gap-1 text-xm" href="javascript:void();" key={index} option={result}>
						<div>Show More</div>
						<ChevronDownCircle className="size-4 stroke-[#aaa]" />
					</MenuItem>
				);
			} else {
				return (
					<MenuItem className="default-menu-item" href="javascript:void();" key={index} option={result}>
						<div>{result.display_name}</div>
					</MenuItem>
				);
			}
		});
		return (
			<Menu className="default-dropdown" {...validMenuProps}>
				{items}
			</Menu>
		);
	}

	const coverageColumns = [
		{ headerName: 'Name', field: 'display_name', flex: 1 }, // Changed field to match API
		{ headerName: 'Role', field: 'corp_title', flex: 1 },
		{ headerName: 'Email', field: 'email', flex: 1 },
		{
			headerName: 'Percentage',
			field: 'percentage',
			flex: 1,
			editable: true,
			cellClass: 'justify-center whitespace-nowrap overflow-hidden text-ellipsis',
			cellEditor: 'agNumberCellEditor',
			cellEditorParams: {
				min: 0,
				max: 100,
			},
		},
		{
			headerName: 'Action',
			field: 'action',
			flex: 1,
			cellClass: 'justify-center whitespace-nowrap overflow-hidden text-ellipsis',
			minWidth: 100,
			filter: false,
			sortable: false,
			headerClass: 'justify-center-custom',
			lockVisible: true,
			suppressHeaderMenuButton: true,
			maxWidth: 120,
			cellRenderer: function CoverageActionCellRenderer(params) {
				return (
					<div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
						<span style={{ cursor: 'pointer', color: '#999' }} title="Delete" onClick={() => params.context.onDeleteCoverage(params.data)}>
							<Trash2 size={18} strokeWidth={1.5} />
						</span>
					</div>
				);
			},
		},
	];

	function handleEditCoverage(row) {
		setSelectedCoverage({ name: row.name, role: row.role, coverageStart: row.coverageStart, coverageEnd: row.coverageEnd });
		setPercentage(row.percentage.toString());
		setCoverageRows((prev) => prev.filter((r) => r.name !== row.name));
	}

	function handleDeleteCoverage(row) {
		setCoverageRows((prev) => prev.filter((r) => r.emp_id !== row.emp_id));
	}

	function handleAddCoverage() {
		// Check if an employee is selected (Typeahead returns an array)
		if (!selectedEmployee || selectedEmployee.length === 0) {
			setCoverageError('Please select an employee.');
			return;
		}

		//  Get the specific employee object from the array
		const employeeData = selectedEmployee[0];
		console.log(employeeData);
		// 3. Validation
		if (percentage === '' || isNaN(Number(percentage))) {
			setCoverageError('Please enter a valid percentage.');
			return;
		}

		// Check for duplicates using email or display_name
		if (coverageRows.some((row) => row.email === employeeData.email)) {
			toast.warning('This employee is already added');
			setSelectedEmployee([]); // Clear Typeahead
			setPercentage('');
			setCoverageError('');
			return;
		}
		const pctValue = Number(percentage);
		console.log(pctValue);

		// Construct the new row merging API data + local percentage
		const newRow = {
			display_name: employeeData.display_name,
			corp_title: employeeData.corp_title,
			email: employeeData.email,
			percentage: pctValue,
		};

		const updatedRows = [...coverageRows, newRow];
		setCoverageRows(updatedRows);

		//Reset UI
		setSelectedEmployee([]);
		setPercentage('');
		setCoverageError('');
	}

	const EmpSplitSchema = z.object({
		emp_id: z.string().nonempty(),
		split: z.string().nonempty(),
	});

	const CommissionSchema = z.object({
		account_id: z.string().nonempty({ message: 'Account ID is required' }),
		comm_date: z.string().nonempty({ message: 'Commission Date is required' }),
		comm_no: z.string().nonempty({ message: 'Commission Number is required' }),

		comm_status: z.enum(['Open', 'Paid', 'Void', 'Pending']),
		commission: z.string().nonempty({ message: 'Commission value is required' }),
		product: z.string().nonempty({ message: 'Product is required' }),
		ticker: z.string().default(''),
		quantity: z.string().default(''),
		price: z.string().default(''),
		payOut: z.boolean().default(false),
		net_amount: z.string().default(''),
		comments: z.string().default(''),
		// Assuming emp_splits is a string representing employee splits data (e.g., JSON string or ID)
		emp_splits: z.array(EmpSplitSchema).default([]),
	});

	// Your existing default values for context (they match the schema structure)
	const defaultValues = {
		account_id: '',
		comm_date: '',
		comm_no: '',
		comm_status: '',
		commission: '',
		product: '',
		ticker: '',
		quantity: '',
		price: '',
		payOut: true,
		net_amount: '',
		comments: '',
		emp_splits: [{ emp_id: '', split: '' }],
	};

	const form = useForm({
		mode: 'onChange',
		defaultValues: defaultValues,
	});

	const watchedStatus = form.watch('product');

	useEffect(() => {
		if (editingRow) {
			form.reset({
				accountname: editingRow.accountName || '',
				invoiceDate: editingRow.date,
				invoiceNumber: editingRow.invoice || '',
				status: editingRow.status || '',
				commission: editingRow.commission ? editingRow.commission.replace(/[^\d.]/g, '') : '',
				includeClientsNotAssigned: false,
				includeDeletedClients: false,
				product: editingRow.product || '',
				invoiceFile: editingRow.invoiceFile || null,
				notes: editingRow.notes || '',
				ticker: editingRow.ticker || '',
				quantity: editingRow.quantity || '',
				price: editingRow.price || '',
				netAmount: editingRow.netAmount || '',
			});
			setSelectedAccount(editingRow.accountName ? [{ accountName: editingRow.accountName }] : []);
			setSheetOpen(true);
		}
	}, [editingRow]);

	function cellEditingStopped(e) {
		if (e.colDef.field === 'percentage') {
			let attemptedValue = e.newValue !== undefined ? Number(e.newValue) : Number(e.value);
			if (!isNaN(attemptedValue) && attemptedValue > 100) {
				toast.error('Percentage cannot be more than 100.');
			}
		}
	}

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
			<TooltipProvider>
				<div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
					<Tooltip>
						<TooltipTrigger asChild>
							<button className="text-gray-400" style={{ background: 'none', border: 'none', cursor: 'pointer' }} onClick={() => setEditingRow(row)}>
								<Pencil size={18} />
							</button>
						</TooltipTrigger>
						<TooltipContent>Edit</TooltipContent>
					</Tooltip>
					<span style={{ color: '#d1d5db', fontWeight: 'bold', fontSize: '18px', userSelect: 'none' }}>|</span>
					<Tooltip>
						<TooltipTrigger asChild>
							<button
								className="text-gray-400"
								style={{ background: 'none', border: 'none', cursor: 'pointer' }}
								onClick={() => {
									setRowToDelete(row);
									setShowDeleteDialog(true);
								}}
							>
								<Trash2 size={18} />
							</button>
						</TooltipTrigger>
						<TooltipContent>Delete</TooltipContent>
					</Tooltip>
				</div>
			</TooltipProvider>
		);
	}

	const prevSheetOpenRef = useRef(sheetOpen);
	useEffect(() => {
		if (prevSheetOpenRef.current && !sheetOpen && form.formState.isDirty) {
			setShowUnsavedDialog(true);
			setSheetOpen(true);
		}
		prevSheetOpenRef.current = sheetOpen;
	}, [sheetOpen, form.formState.isDirty]);

	useEffect(() => {
		if (!sheetOpen) {
			form.reset(defaultValues);
			setSelectedAccount([]);
			setCoverageRows([]);
			setEditingRow(null);
		}
	}, [sheetOpen]);

	function handleSheetOpenChange(nextOpen) {
		if (!nextOpen && form.formState.isDirty) {
			setShowUnsavedDialog(true);
			setSheetOpen(true);
		} else {
			setSheetOpen(nextOpen);
		}
	}

	function handleDialogConfirm() {
		setShowUnsavedDialog(false);
		setSheetOpen(false);
		form.reset();
		setEditingRow(null);
	}

	function handleDialogCancel() {
		setShowUnsavedDialog(false);
		setSheetOpen(true);
	}

	async function handleDeleteConfirm() {
		setShowDeleteDialog(false);
		if (rowToDelete) {
			try {
				setRowData((prev) => prev.filter((row) => row !== rowToDelete));
				setRowToDelete(null);
				toast.success('Record deleted successfully');
			} catch (error) {
				console.error('Error deleting record:', error);
				toast.error('Failed to delete record');
			}
		}
	}

	function handleDeleteCancel() {
		setShowDeleteDialog(false);
		setRowToDelete(null);
	}

	const getRowId = useCallback((params) => params.data.emp_id, []);
	function handleFormSubmit(data) {
		// debugger;
		setIsSaving(true);
		console.log(data);
		const formData = createFormData(data);
		// console.log(formData);

		saveinvoicemutation.mutate(formData);
	}

	return (
		<div className="w-full">
			<Sheet open={sheetOpen} onOpenChange={handleSheetOpenChange}>
				<SheetContent className="sm:max-w-6xl">
					<SheetHeader>
						<SheetTitle>{editingRow ? 'Edit Invoice' : 'Add Invoice'}</SheetTitle>
					</SheetHeader>

					<Dialog open={showUnsavedDialog} onOpenChange={setShowUnsavedDialog}>
						<DialogContent>
							<DialogHeader>
								<DialogTitle>Unsaved Changes</DialogTitle>
							</DialogHeader>
							<div className="mb-6">You have unsaved changes. Are you sure you want to close?</div>
							<DialogFooter className="flex justify-end gap-2">
								<Button variant="outline" onClick={handleDialogCancel}>
									Cancel
								</Button>
								<Button variant="destructive" onClick={handleDialogConfirm}>
									Discard & Close
								</Button>
							</DialogFooter>
						</DialogContent>
					</Dialog>

					<Form {...form}>
						<form className="grid grid-cols-3 gap-6 p-4">
							{/* Row 1 */}
							<div>
								<FormField
									control={form.control}
									name="account_id"
									rules={{ required: 'Account Name is required' }}
									render={({ field }) => (
										<FormItem>
											<FormLabel className="font-medium">
												Account Name<span className="text-red-500">*</span>
											</FormLabel>
											<div style={{ position: 'relative' }}>
												<AsyncTypeahead
													id="account_id"
													labelKey="account_name"
													options={accountOptions}
													isLoading={isLoading}
													minLength={1}
													placeholder="Search Account Name"
													onSearch={handleAccountSearch}
													onChange={handleAccountChange}
													selected={selectedAccount}
													inputProps={{
														className: 'h-11 w-full border rounded-md px-3 py-2 text-base focus:outline-none focus:ring-1 placeholder:text-gray-400',
														required: true,
													}}
													menuStyle={{
														zIndex: 1050,
														width: '100%',
														position: 'absolute',
													}}
													renderMenu={renderAccountMenu}
												/>
											</div>
											<FormMessage />
										</FormItem>
									)}
								/>
							</div>

							<div>
								<FormField
									control={form.control}
									name="comm_date"
									rules={{ required: 'Invoice Date is required' }}
									render={({ field }) => (
										<FormItem>
											<FormLabel className="font-medium">
												Invoice Date<span className="text-red-500">*</span>
											</FormLabel>
											<Popover>
												<PopoverTrigger asChild>
													<Button
														variant="outline"
														className={'w-full justify-start text-left font-normal h-11' + (field.value ? '' : ' text-muted-foreground')}
													>
														<CalendarIcon className="mr-2 h-4 w-4" />
														{field.value ? field.value : <span>Pick a date</span>}
													</Button>
												</PopoverTrigger>
												<PopoverContent className="w-auto p-0">
													<Calendar
														mode="single"
														selected={field.value ? new Date(field.value) : undefined}
														onSelect={(date) => field.onChange(date ? date.toISOString().slice(0, 10) : '')}
														initialFocus
													/>
												</PopoverContent>
											</Popover>
											<FormMessage />
										</FormItem>
									)}
								/>
							</div>

							<div>
								<FormField
									control={form.control}
									name="comm_no"
									rules={{ required: 'Invoice Number is required' }}
									render={({ field }) => (
										<FormItem>
											<FormLabel className="font-medium">
												Invoice Number<span className="text-red-500">*</span>
											</FormLabel>
											<Input
												{...field}
												type="text"
												maxLength={256}
												className="focus:outline-none focus:ring-2 focus:ring-primary h-11"
												placeholder="Enter invoice number"
												required
											/>
											<FormMessage />
										</FormItem>
									)}
								/>
							</div>

							<div>
								<FormField
									control={form.control}
									name="comm_status"
									rules={{ required: 'Invoice Status is required' }}
									render={({ field }) => (
										<FormItem>
											<FormLabel className="font-medium">
												Invoice Status<span className="text-red-500">*</span>
											</FormLabel>
											<Select value={field.value} onValueChange={field.onChange} disabled={field.disabled} required>
												<SelectTrigger className="w-full justify-between h-11">
													<SelectValue placeholder="Select status" />
												</SelectTrigger>
												<SelectContent>
													<SelectItem value="Open">Open</SelectItem>
													<SelectItem value="Paid">Paid</SelectItem>
												</SelectContent>
											</Select>
											<FormMessage />
										</FormItem>
									)}
								/>
							</div>

							<div>
								<FormField
									control={form.control}
									name="commission"
									rules={{ required: 'Commission is required' }}
									render={({ field }) => (
										<FormItem>
											<FormLabel className="font-medium">
												Commission<span className="text-red-500">*</span>
											</FormLabel>
											<Input
												{...field}
												type="number"
												step="any"
												className="w-full focus:outline-none focus:ring-2 focus:ring-primary h-11"
												placeholder="Enter commission amount"
												required
											/>
											<FormMessage />
										</FormItem>
									)}
								/>
							</div>

							<div>
								<FormField
									control={form.control}
									name="product"
									rules={{ required: 'Product is required' }}
									render={({ field }) => (
										<FormItem>
											<FormLabel className="font-medium">
												Product<span className="text-red-500">*</span>
											</FormLabel>
											<Select value={field.value} onValueChange={field.onChange} disabled={field.disabled} required>
												<SelectTrigger className="w-full justify-between h-11">
													<SelectValue placeholder="Select status" />
												</SelectTrigger>
												<SelectContent>
													<SelectItem value="syndicate">Syndicate</SelectItem>
													<SelectItem value="invoice">Invoice</SelectItem>
												</SelectContent>
											</Select>
											<FormMessage />
										</FormItem>
									)}
								/>
							</div>
							{watchedStatus === 'syndicate' && (
								<div className="col-span-3 grid grid-cols-2 gap-6 mt-6">
									<div className="col-span-2 bg-gray-50 dark:bg-gray-800 rounded-lg p-4 grid grid-cols-4 gap-4 mb-4">
										<FormField
											control={form.control}
											name="ticker"
											render={({ field }) => (
												<FormItem>
													<FormLabel className="font-medium">Ticker</FormLabel>
													<Input {...field} type="text" maxLength={32} className="h-10" placeholder="Ticker" />
													<FormMessage />
												</FormItem>
											)}
										/>
										<FormField
											control={form.control}
											name="quantity"
											render={({ field }) => (
												<FormItem>
													<FormLabel className="font-medium">Quantity</FormLabel>
													<Input {...field} type="number" min={0} className="h-10" placeholder="Quantity" />
													<FormMessage />
												</FormItem>
											)}
										/>
										<FormField
											control={form.control}
											name="price"
											render={({ field }) => (
												<FormItem>
													<FormLabel className="font-medium">Price</FormLabel>
													<Input {...field} type="number" min={0} step="any" className="h-10" placeholder="Price" />
													<FormMessage />
												</FormItem>
											)}
										/>
										<FormField
											control={form.control}
											name="netAmount"
											render={({ field }) => (
												<FormItem>
													<FormLabel className="font-medium">Net Amount</FormLabel>
													<Input {...field} type="number" min={0} step="any" className="h-10" placeholder="Net Amount" />
													<FormMessage />
												</FormItem>
											)}
										/>
									</div>
								</div>
							)}

							<div className="col-span-3 grid grid-cols-2 gap-6">
								<FormField
									control={form.control}
									name="invoiceFile"
									render={({ field }) => (
										<FormItem>
											<FormLabel className="font-medium">
												Attach Invoice<span className="text-red-500">*</span>
											</FormLabel>
											<div
												className="border-2 border-dashed dark:border-gray-800 border-gray-300 rounded-lg p-4 dark:bg-inherit flex flex-col items-center justify-center bg-gray-50 hover:bg-gray-100 transition h-[115px] cursor-pointer relative"
												onClick={() => document.getElementById('invoiceFileInput')?.click()}
											>
												<input
													type="file"
													id="invoiceFileInput"
													accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx"
													className="hidden"
													onChange={(e) => field.onChange(e.target.files && e.target.files[0])}
												/>
												{field.value ? (
													<div className="flex flex-col items-center w-full">
														<span className="text-sm text-gray-700 truncate max-w-xs">{field.value.name}</span>
														<Button
															type="button"
															variant="outline"
															size="sm"
															className="mt-2"
															onClick={(e) => {
																e.stopPropagation();
																field.onChange(null);
															}}
														>
															Remove
														</Button>
													</div>
												) : (
													<span className="text-gray-400 text-sm">Drag & drop or click to upload (PDF, Word, PPT, XLS)</span>
												)}
											</div>
											<FormMessage />
										</FormItem>
									)}
								/>

								<FormField
									control={form.control}
									name="comments"
									render={({ field }) => (
										<FormItem>
											<FormLabel>Notes</FormLabel>
											<textarea id="notes" rows={4} className="border rounded w-full p-2" placeholder="Enter notes here..." {...field} />
											<FormMessage />
										</FormItem>
									)}
								/>
							</div>
						</form>
					</Form>

					{/* Only visible the coverage details once the Account name selected */}
					{selectedAccount.length > 0 && (
						<div className="p-4">
							<h3 className="text-lg font-semibold mb-2">Coverage Details</h3>

							<div className="mb-4 flex gap-2 items-center" style={{ maxWidth: 500 }}>
								<div style={{ position: 'relative', flex: 2 }}>
									<AsyncTypeahead
										id="employee_search"
										labelKey={(option) => option.display_name}
										options={employeeOptions}
										isLoading={isEmployeeLoading}
										minLength={1}
										placeholder="Search employee name..."
										onSearch={handleEmployeeSearch}
										onChange={handleEmployeeChange}
										selected={selectedEmployee}
										inputProps={{
											className: 'h-10 w-80 border rounded-md px-3 py-2 text-base focus:outline-none focus:ring-1 placeholder:text-gray-400',
										}}
										menuStyle={{
											zIndex: 1050,
											width: '100%',
											position: 'absolute',
										}}
										renderMenu={renderEmployeeMenu}
									/>
									{isEmployeeLoading && selectedEmployee.length > 0 && (
										<ul
											className="dark:bg-gray-800 bg-gray-100 border w-80"
											style={{
												position: 'absolute',
												zIndex: 10,
												borderRadius: 4,
												margin: 0,
												padding: 0,
												width: '100%',
												maxHeight: 120,
												overflowY: 'auto',
												listStyle: 'none',
											}}
										></ul>
									)}
								</div>

								{selectedEmployee && selectedEmployee.length > 0 && (
									<div className="flex items-center mt-2">
										<Input
											value={percentage}
											onChange={(e) => {
												const val = e.target.value.replace(/[^0-9.]/g, '');
												setPercentage(val);
												if (val !== '' && Number(val) > 100) {
													setPercentageInputError('Percentage cannot be more than 100.');
												} else {
													setPercentageInputError('');
												}
											}}
											type="number"
											min={0}
											max={100}
											placeholder="%"
											className="input input-bordered border rounded px-3 py-2 text-base w-24"
											disabled={!selectedEmployee}
										/>
										<Button
											type="button"
											variant="default"
											className="ml-2"
											onClick={handleAddCoverage}
											disabled={!selectedEmployee || percentage === '' || (percentage !== '' && Number(percentage) > 100)}
										>
											Add
										</Button>
									</div>
								)}
							</div>
							{coverageRows.length > 0 && (
								<div
									className={`${IS_DARK.value ? 'ag-theme-balham-dark' : 'ag-theme-balham'}  coverage-table w-full`}
									style={{ height: 220, minHeight: 120, overflow: 'auto' }}
								>
									<AgGridReact
										columnDefs={coverageColumns}
										getRowId={getRowId}
										rowData={coverageRows}
										rowHeight={34}
										headerHeight={34}
										domLayout="normal"
										modules={[ClientSideRowModelModule]}
										defaultColDef={defaultColDef.filter == ''}
										suppressMovableColumns
										context={{ onEditCoverage: handleEditCoverage, onDeleteCoverage: handleDeleteCoverage }}
										onCellEditingStopped={cellEditingStopped}
										noRowsOverlayComponent={() => <span>Record not found.</span>}
										invalidEditValueMode={'block'}
									/>
								</div>
							)}
						</div>
					)}
					<SheetFooter className="flex flex-row justify-end">
						<Button
							className={`${!form.formState.isValid ? 'opacity-50 cursor-not-allowed' : ''}`}
							ref={null}
							type="submit"
							onClick={form.handleSubmit(handleFormSubmit)}
						>
							Save
						</Button>
						<Button variant="outline" type="button" onClick={() => handleSheetOpenChange(false)}>
							Cancel
						</Button>
					</SheetFooter>
				</SheetContent>
			</Sheet>
			<div className="flex justify-between pb-2">
				<DateRangePicker
					onCancel={dateRangeCancelHandler}
					locale="en-US"
					// initialDateFrom={rangeStartDate}
					// initialDateTo={rangeEndDate}
					onUpdate={dateRangeHandler}
					showCompare={false}
				></DateRangePicker>
				<div className="flex gap-2 items-center">
					<span className=" font-medium mr-2">#{rowData.length} Records</span>
					<Button title={'Refresh'} className={'h-[40px]'} variant="outline">
						<RefreshCcwIcon></RefreshCcwIcon>
					</Button>
					<Button onClick={() => toast.success('Download Completed')} title={'Download'} className={'h-[40px] border-custom-border'} variant="outline">
						<Download></Download>
						Excel
					</Button>
				</div>
			</div>
			<div
				className={`${IS_DARK.value ? 'ag-theme-balham-dark' : 'ag-theme-balham'} w-full grid-container invoice-table`}
				style={{ height: 700, minHeight: 300, overflow: 'auto' }}
			>
				<AgGridReact
					ref={gridRef}
					columnDefs={columns.map((col) => {
						if (col.cellRenderer === 'ActionCellRenderer') return { ...col, cellRenderer: ActionCellRenderer };
						if (col.cellRenderer === 'StatusCellRenderer') return { ...col, cellRenderer: StatusCellRenderer };
						return { ...col, cellRenderer: DefaultCellRenderer };
					})}
					defaultColDef={defaultColDef}
					rowData={rowData}
					modules={[ColumnsToolPanelModule, SetFilterModule, MenuModule, ClientSideRowModelModule]}
					rowHeight={40}
					headerHeight={40}
					domLayout="normal"
					sideBar={sideBar}
					suppressMovableColumns
					noRowsOverlayComponent={() => <span>Record not found.</span>}
				/>
			</div>
			{/* Delete warning dialog using shadcn Dialog */}
			<Dialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
				<DialogContent>
					<DialogHeader>
						<DialogTitle>Delete Invoice</DialogTitle>
					</DialogHeader>
					<div className="mb-6">Are you sure you want to delete this invoice? This action cannot be undone.</div>
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
		</div>
	);
}
