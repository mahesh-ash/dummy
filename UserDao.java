import React, { useRef, useState, useMemo, useEffect, useCallback } from 'react';
import { AsyncTypeahead } from 'react-bootstrap-typeahead';
import { AgGridReact } from '@ag-grid-community/react';
import { ColumnsToolPanelModule } from '@ag-grid-enterprise/column-tool-panel';
import { SetFilterModule } from '@ag-grid-enterprise/set-filter';
import { MenuModule } from '@ag-grid-enterprise/menu';
import { ClientSideRowModelModule } from '@ag-grid-community/client-side-row-model';
import { Eye, Pencil, Trash2, Plus, Download, CircleDot, CheckCircle2, Clock, RefreshCcw, RefreshCcwIcon, LucideDownload } from 'lucide-react';
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
import { AUTH_TOKEN, DATE_RANGE, INVOICE_IMAGE_VARIABLE, IS_DARK } from '@/store/signals';
import { useSignals } from '@preact/signals-react/runtime';
import { DateRangePicker } from '../common/DateRangePicker';
import { useMutation, useQuery } from 'react-query';
import { deleteInvoiceMutation, getAllCommission, getConstant, getInvoiceDetails, saveinvoicemutations, updateinvoicemutations } from '@/utils/service';
import z from 'zod';
import { ServerSideRowModelModule } from '@ag-grid-enterprise/server-side-row-model';
import { format, parseISO } from 'date-fns';
import { Spinner } from '../common/Spinner';
import { convertDate, getDateRange } from '@/common/helpers/DateTimeFormat';
import { createFormData, formatDate } from '@/utils/common';
import { RangeSelectionModule } from '@ag-grid-enterprise/range-selection';
import { ClipboardModule } from '@ag-grid-enterprise/clipboard';

const defaultColDef = {
	sortable: true,
	editable: false,
	flex: 1,
	filter: 'agTextColumnFilter', // Enable text filter for all columns
	floatingFilter: true,
	cellStyle: {
		whiteSpace: 'nowrap',
		overflow: 'hidden',
		textOverflow: 'ellipsis',
	},
};

const dynamicRowFields = INVOICE_IMAGE_VARIABLE.value.formConfig?.extraFormFields.map((f, index) => ({
	name: f?.name || `dyn_field_${index}`,
	label: f?.label || `Field ${index + 1}`,
	placeholder: f?.placeholder || '',
	type: f?.type || 'string',
	mandatory: f?.mandatory ?? false,
	pattern: f?.pattern || null,
	patternMessage: f?.message || '',
	readOnly: f?.readOnly || false,
	disabled: f?.disabled || false,
	defaultValue: f?.defaultValue ?? '',
	className: f?.className || '',
}));

const dynamicRowDefaults = dynamicRowFields.reduce((acc, field) => {
	acc[field.name] = field.defaultValue ?? (field.type === 'number' ? undefined : '');
	return acc;
}, {});

const getDynamicRules = (field) => {
	const rules = {};

	if (field.mandatory && !field.readOnly && !field.disabled) {
		rules.required = `${field.label} is required`;
	}

	if (field.pattern && !field.disabled) {
		rules.pattern = {
			value: field.pattern,
			message: field.patternMessage || 'Invalid format',
		};
	}

	return rules;
};

const columns = [
	{
		headerName: 'Account Name',
		field: 'account_name',
		// filter: 'agTextColumnFilter',
		filterParams: {
			filterPlaceholder: (params) => {
				return 'Search Account Name';
			},
		},
		lockVisible: true,
		minWidth: 350,
	},
	{ headerName: 'Account ID', minWidth: 200, field: 'acc_id', cellClass: 'justify-center whitespace-nowrap overflow-hidden text-ellipsis' },
	{
		headerName: 'Invoice Date',
		field: 'invoice_date',
		headerClass: 'justify-center',
		minWidth: 200,
		filter: false,
		cellClass: 'justify-center whitespace-nowrap overflow-hidden text-ellipsis',
	}, // Disable filter for date column
	{
		headerName: 'Commission',
		minWidth: 200,
		field: 'commission',
		cellRenderer: 'CurrencyCellRenderer',
	},
	{
		headerName: 'Status',
		minWidth: 200,
		field: 'invoice_status',
		cellRenderer: 'StatusCellRenderer',
		cellClass: 'justify-center whitespace-nowrap overflow-hidden text-ellipsis',
	},
	// { headerName: 'Invoice', field: 'invoice', cellClass: 'justify-center whitespace-nowrap overflow-hidden text-ellipsis' },
	{ headerName: 'Product', minWidth: 200, field: 'product' },
	{ headerName: 'Quantity', minWidth: 200, field: 'quantity' },
	{ headerName: 'Payout', minWidth: 200, field: 'payout' },
	{ headerName: 'Comments', minWidth: 200, field: 'comments' },
	{ headerName: 'Net Amount', minWidth: 200, field: 'net_amount' },
	{ headerName: 'Invoice Number', minWidth: 200, field: 'invoice_no' },
	{
		headerName: 'Action',
		field: 'action',
		filter: false,
		sortable: false,
		editable: false,
		menuTabs: [], // disables column menu
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

function CurrencyCellRenderer(params) {
	const value = params.value;
	const formattedValue = new Intl.NumberFormat('en-US', {
		style: 'currency',
		currency: 'USD',
	}).format(value);
	return <div title={formattedValue}>{formattedValue}</div>;
}

function DefaultCellRenderer(props) {
	const col = props.colDef;
	// Custom Invoice column renderer with download icon on hover
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
						/* TODO: implement download logic if needed */
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
	const status = props.value.toLowerCase();
	const isDark = IS_DARK.value;
	let color = isDark ? 'bg-gray-800 text-gray-200' : 'bg-gray-200 text-gray-800';
	let icon = <CircleDot size={16} className={isDark ? 'mr-1 text-gray-400' : 'mr-1'} />;
	if (status === 'open') {
		color = isDark ? 'bg-yellow-900 text-yellow-200' : 'bg-yellow-100 text-yellow-800';
		icon = <Clock size={16} className={isDark ? 'mr-1 text-yellow-300' : 'mr-1 text-yellow-600'} />;
	}
	if (status === 'paid') {
		color = isDark ? 'bg-green-900 text-green-200' : 'bg-green-100 text-green-800';
		icon = <CheckCircle2 size={16} className={isDark ? 'mr-1 text-green-300' : 'mr-1 text-green-600'} />;
	}
	if (status === 'pending') {
		color = isDark ? 'bg-blue-900 text-blue-200' : 'bg-blue-100 text-blue-800';
		icon = <CircleDot size={16} className={isDark ? 'mr-1 text-blue-300' : 'mr-1 text-blue-600'} />;
	}
	if (status === 'active') {
		color = isDark ? 'bg-cyan-900 text-cyan-200' : 'bg-cyan-100 text-cyan-800';
		icon = <CheckCircle2 size={16} className={isDark ? 'mr-1 text-cyan-300' : 'mr-1 text-cyan-600'} />;
	}
	return (
		<span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold gap-1 ${color}`} style={{ minWidth: 80 }}>
			{icon}
			{props.value}
		</span>
	);
}

export default function ManageInvoiceTable(props) {
	// Typeahead for Account Name
	useSignals();
	const saveinvoicemutation = useMutation({
		mutationFn: saveinvoicemutations,
	});

	const updateinvoicemutation = useMutation({
		mutationFn: updateinvoicemutations,
	});

	const getinvoiceDetailss = useMutation({
		mutationFn: getInvoiceDetails,
	});

	// Loading states
	const [isLoadingData, setIsLoadingData] = useState(false);
	const [isSaving, setIsSaving] = useState(false);

	// Typeahead for Account Name
	const [accountOptions, setAccountOptions] = useState([]);
	const [accountQuery, setAccountQuery] = useState('');
	const [rangeStartDate, setRangeStartDate] = useState(new Date());
	const [rangeEndDate, setRangeEndDate] = useState('');
	const [selectedAccount, setSelectedAccount] = useState([]);
	const [sheetOpen, setSheetOpen] = useState(false);

	// Grid and sheet states
	const gridRef = useRef();
	const [rowData, setRowData] = useState([]);
	const [showUnsavedDialog, setShowUnsavedDialog] = useState(false);
	const [editingRow, setEditingRow] = useState(null);
	const [showDeleteDialog, setShowDeleteDialog] = useState(false);
	const [rowToDelete, setRowToDelete] = useState(null);

	const [open, setOpen] = useState(false);

	const [dialogConfig, setDialogConfig] = useState({
		isOpen: false,
		title: '',
		message: '',
		onConfirm: () => {},
	});

	// excell downloading
	const [excelLoading, setExcelLoading] = useState(false);

	// Coverage states

	const [columnDefs, setColumnDefs] = useState([]);
	const [showDropdown, setShowDropdown] = useState(false);
	const [selectedCoverage, setSelectedCoverage] = useState(null);
	const [percentage, setPercentage] = useState('');
	const [coverageRows, setCoverageRows] = useState([]);
	const [coverageError, setCoverageError] = useState('');
	const [percentageInputError, setPercentageInputError] = useState('');
	const [selectedEmployee, setSelectedEmployee] = useState([]);
	const [lastRefresh, setLastRefresh] = useState(null);

	const [disableSaveBtn, setDisableSaveBtn] = useState(false);

	const deleteinvoicemutations = useMutation({
		mutationFn: deleteInvoiceMutation,
	});

	useEffect(() => {
		if (percentageInputError != '') {
			toast.error(percentageInputError);
		}
	}, [percentageInputError]);

	const [employeeQuery, setEmployeeQuery] = useState('');
	const [employeeOptions, setEmployeeOptions] = useState([]);
	const [isEmployeeLoading, setIsEmployeeLoading] = useState(false);

	const [isLoading, setIsLoading] = useState(false);

	async function handleEmployeeSearch(query) {
		setEmployeeQuery(query);
		setIsEmployeeLoading(true);
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
		// debugger;
    if (updateinvoicemutation.data && updateinvoicemutation.data.status === 1) {
        // toast.success('Invoice updated successfully');
        setSheetOpen(false);
        setEditingRow(null);
        form.reset(defaultValues);
        setCoverageRows([]);

        if (gridRef.current?.api) {
            gridRef.current.api.refreshServerSide();
            
            gridRef.current.api.deselectAll();
        }
    } else if (updateinvoicemutation.data && updateinvoicemutation.data.status === 0) {
        toast.error(updateinvoicemutation.data.message || 'Update failed');
        setDisableSaveBtn(false);
    }
    setIsSaving(false);
}, [updateinvoicemutation.isSuccess]);


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

	useEffect(() => {
		// debugger;
		if (deleteinvoicemutations.data && deleteinvoicemutations.data.status === 1) {
			setShowDeleteDialog(false);
			setRowToDelete(null);
			// Refresh the grid
			const datasource = createDataSource();
			gridRef.current?.api?.refreshServerSide({ purge: false });
			gridRef.current?.api?.setGridOption('serverSideDatasource', datasource);
			// toast.success('Invoice deleted successfully');
		} else {
			setShowDeleteDialog(false);
			// toast.error('Failed to delete invoice');
		}
	}, [deleteinvoicemutations.isSuccess]);

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

	function formatDateTime(dateStr, timeStr) {
		// Accepts yyyy-mm-dd or ISO, returns mm/dd/yyyy and time
		const d = new Date(timeStr || dateStr);
		if (isNaN(d)) return dateStr;
		const mm = String(d.getMonth() + 1).padStart(2, '0');
		const dd = String(d.getDate()).padStart(2, '0');
		const yyyy = d.getFullYear();
		let hours = d.getHours();
		const minutes = String(d.getMinutes()).padStart(2, '0');
		const ampm = hours >= 12 ? 'PM' : 'AM';
		hours = hours % 12;
		hours = hours ? hours : 12; // the hour '0' should be '12'
		const time = `${hours}:${minutes} ${ampm}`;
		return `${mm}/${dd}/${yyyy} ${time}`;
	}

	// Format date as yyyy-MM-dd
	function formatDate(date) {
		if (!date) return '';
		const d = new Date(date);
		return d.toISOString().slice(0, 10);
	}
	const downloadEBBExcel = async (fileName, data) => {
		try {
			setExcelLoading(true);
			// toast.success('Downloading.....');
			const response = await fetch(`${import.meta.env.VITE_API_URL}commission/GetAllCommissionexport`, {
				method: 'POST',
				headers: {
					auth_token: encodeURIComponent(AUTH_TOKEN.value),
					// Add authorization if needed
					'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
				},
				body: new URLSearchParams(data),
			});

			if (!response.ok) {
				console.error('Something went wrong');
				throw new Error('Network response was not ok');
			}

			let headerFileName = response.headers.get('content-disposition');
			if (headerFileName) {
				const fileNameMatch = headerFileName.match(/filename="?([^"]+)"?/);
				if (fileNameMatch) {
					headerFileName = fileNameMatch[1];
				}
			}
			const blob = await response.blob();
			const url = window.URL.createObjectURL(blob);

			const a = document.createElement('a');
			a.href = url;
			a.download = headerFileName ? headerFileName : `${fileName}.xlsx`; // default filename
			document.body.appendChild(a);
			a.click();
			a.remove();
			toast.success('Download Completed');
			window.URL.revokeObjectURL(url);
			setExcelLoading(false);
		} catch (error) {
			toast.error('Download Failed');
			setExcelLoading(false);
			console.error('Download failed:', error);
		}
	};

	useEffect(() => {
		if (DATE_RANGE.value && DATE_RANGE.value.range) {
			setRangeStartDate(formatDate(getDateRange(DATE_RANGE.value.range).start, false));
			setRangeEndDate(formatDate(getDateRange(DATE_RANGE.value.range).end, false));
		}
	}, [DATE_RANGE.value]);

	function excelDownloadHandler() {
		const columnState = gridRef.current?.api?.getColumnState();
		const sortModel = columnState
			.filter((col) => col.sort != null) // Filter for columns with sorting applied
			.map((col) => ({
				colId: col.colId,
				sort: col.sort,
			}));
		const clientTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
		const obj = {
			start: 0,
			// length: sortModel.length ? JSON.stringify(sortModel[0]) : '',
			start_date: rangeStartDate,
			end_date: rangeEndDate,
			filters: '',
			// order:sortModel.length ? JSON.stringify(sortModel[0]) : '',
			order: '',
		};
		downloadEBBExcel('getInvoiceData', createFormData(obj));
	}

	function dateRangeHandler(value) {
		// value: { range: { from, to }, rangeCompare }
		const from = value?.range?.from ? formatDate(value.range.from) : '';
		const to = value?.range?.to ? formatDate(value.range.to) : '';
		setRangeStartDate(from);
		setRangeEndDate(to);
		// Update grid datasource with new date range and current search
		if (gridRef.current && gridRef.current.api) {
			const datasource = createDataSource(from, to);
			gridRef.current.api.deselectAll && gridRef.current.api.deselectAll();
			gridRef.current.api.setGridOption('serverSideDatasource', datasource);
			gridRef.current.api.refreshServerSide && gridRef.current.api.refreshServerSide({ purge: true });
		}
	}

	function dateRangeCancelHandler() {
		const today = formatDate(new Date());
		setRangeStartDate(today);
		setRangeEndDate(today);
		if (gridRef.current && gridRef.current.api) {
			const datasource = createDataSource(today, today);
			gridRef.current.api.deselectAll && gridRef.current.api.deselectAll();
			gridRef.current.api.setGridOption('serverSideDatasource', datasource);
			gridRef.current.api.refreshServerSide && gridRef.current.api.refreshServerSide({ purge: true });
		}
	}

	const handleEmployeeChange = (selected) => {
		if (selected.length > 0) {
			const employeeData = selected[0];

			// Check if employee email exists in the Ag-Grid rows
			const isAlreadyAdded = coverageRows.some((row) => row.email === employeeData.email);

			if (isAlreadyAdded) {
				// Show toast and reset selection immediately
				toast.warning(`Employee is already in the coverage list.`);
				setSelectedEmployee([]); // Resets the Typeahead
				setPercentage(''); // Clears percentage
				return;
			}

			// If not a duplicate, proceed normally
			setSelectedEmployee(selected);
		} else {
			setSelectedEmployee([]);
		}
	};

	function createDataSource(fromDate = '', toDate = '') {
		return {
			getRows: async (requestParams) => {
				const { startRow, endRow } = requestParams.request;
				const limit = endRow - startRow; // page size
				const formDataObj = { start: startRow, length: limit };
				if (requestParams.request.sortModel.length > 0) {
					formDataObj.order = JSON.stringify(requestParams.request.sortModel[0]);
				}
				const handleApiResponse = (apiPromise) => {
					return apiPromise
						.then((res) => {
							// Validate response structure
							if (!res || typeof res !== 'object') {
								throw new Error('Invalid response format');
							}

							const rowData = Array.isArray(res.data) ? res.data : [];
							const totalCount = typeof res.total_count === 'number' ? res.total_count : rowData.length;
							setLastRefresh(new Date());
							requestParams.success({
								rowData: rowData,
								rowCount: totalCount,
							});

							// Handle overlay after success callback
							setTimeout(() => {
								if (gridRef.current?.api) {
									if (rowData.length === 0) {
										gridRef.current.api.showNoRowsOverlay();
									} else {
										gridRef.current.api.hideOverlay();
									}
								}
							}, 100);
						})
						.catch((error) => {
							console.error('API Error:', error);
							// Check if still on same tab before showing error
							if (gridRef.current?.api) {
								gridRef.current.api.showNoRowsOverlay();
							}
							requestParams.success({
								rowData: [],
								rowCount: 0,
							});
						})
						.finally(() => {});
				};
				if (fromDate) {
					formDataObj.start_date = fromDate;
				}
				if (toDate) {
					formDataObj.end_date = toDate;
				}
				if (requestParams.request.filterModel && Object.keys(requestParams.request.filterModel).length !== 0) {
					formDataObj.filters = JSON.stringify(requestParams.request.filterModel);
				}

				const formData = createFormData(formDataObj);
				handleApiResponse(getAllCommission(formData));
			},
		};
	}

	useEffect(() => {
		setColumnDefs(columns);
	}, []);

	useEffect(() => {
		setTimeout(() => {
			const datasource = createDataSource(); // Clear search text for tab/dropdown changes
			gridRef.current?.api?.setGridOption('serverSideDatasource', datasource);
		}, 1000);
	}, [columnDefs]);

	async function handleAccountChange(selected) {
		if (coverageRows.length > 0) {
			setDialogConfig({
				isOpen: true,
				title: 'Confirm Account Change',
				message: 'Changing the Account Name will remove currently listed employees. Proceed?',
				onConfirm: () => executeAccountChange(selected),
			});
			return;
		}
		await executeAccountChange(selected);
	}
	async function executeAccountChange(selected) {
		setSelectedAccount(selected);

		if (selected.length > 0) {
			const accountId = selected[0].account_id;
			form.setValue('acc_id', accountId, { shouldValidate: true });

			try {
				const response = await httpClient.get(`Search/GetSearchText?searchText=${accountId}&type=employeeByAccIDQuery`);

				if (response.data?.data?.length > 0) {
					const employees = response.data.data;
					setCoverageRows(employees);
					const splitsArray = employees.map((emp) => ({
						emp_id: emp.emp_id.toString(),
						split: '',
					}));
					form.setValue('emp_splits', JSON.stringify(splitsArray), { shouldValidate: true });
				} else {
					setCoverageRows([]);
					form.setValue('emp_splits', JSON.stringify([{ emp_id: '', split: '' }]), { shouldValidate: true });
				}
			} catch (error) {
				setCoverageRows([]);
				form.setValue('emp_splits', JSON.stringify([{ emp_id: '', split: '' }]));
			}
		} else {
			// This runs when the user clears the account name
			form.setValue('acc_id', '');
			setCoverageRows([]);
			form.setValue('emp_splits', JSON.stringify([{ emp_id: '', split: '' }]));
			toast.info('Account removed and employees cleared.');
		}
	}

	function renderAccountMenu(results, menuProps) {
		if (accountQuery.length === 0 && accountOptions.length === 0) {
			return null;
		}

		// DESTRICTURING: Remove the props React is complaining about
		const { paginationText, newSelectionPrefix, renderMenuItemChildren, ...validMenuProps } = menuProps;

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
		{ headerName: 'Name', field: 'display_name', flex: 1 },
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

	// Your existing default values for context (they match the schema structure)
	const defaultValues = {
		acc_id: '',
		invoice_date: '',
		invoice_no: '',
		invoice_status: '',
		commission: '',
		product: '',
		ticker: '',
		quantity: '',
		price: '',
		payOut: true,
		net_amount: '',
		comments: '',
		attach: '',
		att_name: '',
		att_size: '',
		emp_splits: [{ emp_id: '', split: '' }],
	};

	const form = useForm({
		mode: 'onChange',
		defaultValues: {
			...defaultValues,
			...dynamicRowDefaults,
		},
	});

	const watchedStatus = form.watch('product');

	async function fetchEmployeesForAccount(accountId) {
		try {
			const response = await httpClient.get(`Search/GetSearchText?searchText=${accountId}&type=employeeByAccIDQuery`);
			if (response.data?.data) {
				setCoverageRows(response.data.data);
			}
		} catch (error) {
			console.error('Failed to fetch employees', error);
		}
	}

	useEffect(() => {
    if (getinvoiceDetailss.data && getinvoiceDetailss.isSuccess) {
        const rowData = getinvoiceDetailss.data;
        console.log(rowData);
        let empSplitsArray = [];
        if (rowData.emp_splits) {
            try {
                empSplitsArray = typeof rowData.emp_splits === 'string' 
                    ? JSON.parse(rowData.emp_splits) 
                    : rowData.emp_splits;
            } catch (e) {
                console.error('Error parsing emp_splits:', e);
            }
        }


        // This populates your table immediately
        const mappedRows = empSplitsArray.map(emp => ({
            ...emp,
            emp_id: emp.emp_id,
            display_name: emp.emp_name, 
            corp_title: emp.role_name,   
            email: emp.emp_email,       
            percentage: emp.percentage   
        }));
        
        setCoverageRows(mappedRows); 
        if (rowData.acc_id && rowData.account_name) {
            setSelectedAccount([
                {
                    account_id: rowData.acc_id,
                    account_name: rowData.account_name,
                },
            ]);
        }

        form.reset({
				acc_id: rowData.acc_id || '',
				product: rowData.product || '',
				invoice_date: rowData.invoice_date || '',
				invoice_no: rowData.invoice_no || '',
				invoice_status: rowData.invoice_status || '',
				commission: rowData.commission || '',
				ticker: rowData.ticker || '',
				quantity: rowData.quantity || '',
				price: rowData.price || '',
				payOut: rowData.payOut !== undefined ? rowData.payOut : true,
				net_amount: rowData.net_amount || '',
				comments: rowData.comments || '',
				attach: rowData.attach || '',
				att_name: rowData.att_name || '',
				att_size: rowData.att_size || '',
				emp_splits: empSplitsArray,
				...dynamicRowFields.reduce((acc, field) => {
					if (rowData[field.name] !== undefined) {
						acc[field.name] = rowData[field.name];
					}
					return acc;
				}, {}),
			});
        
        setSheetOpen(true);
    }
}, [getinvoiceDetailss.isSuccess, getinvoiceDetailss.data]);


	


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
							<button
								className="text-gray-400"
								style={{ background: 'none', border: 'none', cursor: 'pointer' }}
								onClick={() => {
									setEditingRow(row); // Set editing row first
									getinvoiceDetailss.mutate({ invoice_no: row.invoice_no });
									// Don't open sheet here - let useEffect handle it
								}}
							>
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
			// Keep sheet open until dialog resolves
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
		if (rowToDelete && rowToDelete.invoice_no) {
			deleteinvoicemutations.mutate({ invoice_no: rowToDelete.invoice_no });
		}
	}

	function handleDeleteCancel() {
		setShowDeleteDialog(false);
		setRowToDelete(null);
	}

	// to remove the undefined with empty values
	const sanitizeData = (obj) => {
		return Object.keys(obj).reduce(
			(acc, key) => {
				const value = obj[key];
				// If it's a nested object (and not null), recurse; otherwise check for undefined
				acc[key] = typeof value === 'object' && value !== null ? sanitizeData(value) : value === undefined ? '' : value;
				return acc;
			},
			Array.isArray(obj) ? [] : {}
		);
	};

	async function handleUpdateSubmit(data) {
		const cleanData = sanitizeData(data);
		const totalPercentage = coverageRows.reduce((sum, row) => {
			return sum + (Number(row.percentage) || 0);
		}, 0);

		// Strict Validation: Must be exactly 100
		if (totalPercentage !== 100) {
			toast.error(`Total allocation must be exactly 100%. Current total: ${totalPercentage}%`);
			return;
		}

		const validEmployees = coverageRows.filter((row) => (Number(row.percentage) || 0) > 0);

		const filteredSplits = validEmployees.map((emp) => ({
			emp_id: emp.emp_id.toString(),
			split: emp.percentage.toString(),
		}));

		const updatedData = {
			...cleanData,
			emp_splits: JSON.stringify(filteredSplits),
			invoice_no: editingRow?.invoice_no || cleanData.invoice_no,
		};

		setIsSaving(true);
		const formData = createFormData(updatedData);
		updateinvoicemutation.mutate(formData);
	}

	// to maintain the same date format for invoice_date also
	function formatDateTime(dateStr, timeStr) {
		// Accepts yyyy-mm-dd or ISO, returns mm/dd/yyyy and time
		const d = new Date(timeStr || dateStr);
		if (isNaN(d)) return dateStr;
		const mm = String(d.getMonth() + 1).padStart(2, '0');
		const dd = String(d.getDate()).padStart(2, '0');
		const yyyy = d.getFullYear();
		let hours = d.getHours();
		const minutes = String(d.getMinutes()).padStart(2, '0');
		const ampm = hours >= 12 ? 'PM' : 'AM';
		hours = hours % 12;
		hours = hours ? hours : 12; // the hour '0' should be '12'
		const time = `${hours}:${minutes} ${ampm}`;
		return `${mm}/${dd}/${yyyy} ${time}`;
	}

	const getRowId = useCallback((params) => params.data.emp_id, []);
	function handleFormSubmit(data) {
    const cleanData = sanitizeData(data);

    // 1. Calculate total using Number() to handle strings from inputs
    const totalPercentage = coverageRows.reduce((sum, row) => {
        const val = parseFloat(row.percentage) || 0;
        return sum + val;
    }, 0);

    // 2. Validate total (Strict 100)
    if (totalPercentage !== 100) {
        toast.error(`Total allocation must be 100%. Currently: ${totalPercentage}%`);
        return;
    }

    // 3. Map values and convert to Strings for your API requirements
    const filteredSplits = coverageRows
        .filter((row) => (parseFloat(row.percentage) || 0) > 0)
        .map((emp) => ({
            emp_id: emp.emp_id.toString(),
            split: emp.percentage.toString(),
        }));

    const updatedData = {
        ...cleanData,
        emp_splits: JSON.stringify(filteredSplits),
    };

    setIsSaving(true);
    const formData = createFormData(updatedData);
    saveinvoicemutation.mutate(formData);
}


	const onCellValueChanged = (params) => {
		// This ensures that when the user types a percentage,
		// the 'coverageRows' state is updated immediately.
		const updatedRows = [...coverageRows];
		const index = updatedRows.findIndex((row) => row.emp_id === params.data.emp_id);
		if (index > -1) {
			updatedRows[index] = params.data;
			setCoverageRows(updatedRows);
		}
	};

	function handleRefresh() {
		if (gridRef.current) {
			gridRef.current?.api?.refreshServerSide({ purge: true });
		}
	}

	// Map the dynamic array into a lookup object with defaults
	const fieldConfigs = (INVOICE_IMAGE_VARIABLE.value.formConfig?.formFields || []).reduce((acc, field) => {
		acc[field.name] = {
			mandatory: field.mandatory ?? true,
			disabled: field.disabled ?? false,
			hidden: field.hidden ?? false,
			options: field.options || [],
		};
		return acc;
	}, {});

	// common function to get the configurations from dynamically
	const getConfig = (fieldName) => fieldConfigs[fieldName] || { mandatory: true, disabled: false, hidden: false, options: [] };

	// y

	return (
		<div className="w-full">
			<div className="flex justify-end gap-4 items-center pb-2">
				<TooltipProvider>
					<Tooltip>
						<TooltipTrigger asChild>
							<Button onClick={handleRefresh} title={'Refresh'} className={'h-[40px]'} variant="outline">
								<RefreshCcwIcon className={`stroke-[#9ca3af]`}></RefreshCcwIcon>
							</Button>
						</TooltipTrigger>

						<TooltipContent>Last Refreshed: {convertDate(lastRefresh)}</TooltipContent>
					</Tooltip>
				</TooltipProvider>
				<DateRangePicker
					onCancel={dateRangeCancelHandler}
					locale="en-US"
					initialDateFrom={rangeStartDate}
					initialDateTo={rangeEndDate}
					onUpdate={dateRangeHandler}
					showCompare={false}
				></DateRangePicker>
				<Button onClick={excelDownloadHandler} className={'h-[40px]'} variant={'outline'}>
					{!excelLoading && <LucideDownload className={`stroke-[#9ca3af]`}></LucideDownload>}
					{excelLoading && <Spinner></Spinner>}
					Excel
				</Button>
				<Button onClick={() => props?.onHistory && props.onHistory()} variant="outline" className="gap-2 h-[40px]">
					History
				</Button>
				<Button onClick={() => setSheetOpen(true)} variant="default" className="gap-2 h-[40px]">
					<Plus size={18} /> Add
				</Button>
			</div>
			<div
				className={`${IS_DARK.value ? 'ag-theme-balham-dark' : 'ag-theme-balham'} w-full grid-container invoice-table`}
				style={{ height: 700, minHeight: 300, overflow: 'auto' }}
			>
				<AgGridReact
					ref={gridRef}
					columnDefs={columnDefs.map((col) => {
						if (col.cellRenderer === 'ActionCellRenderer') return { ...col, cellRenderer: ActionCellRenderer };
						if (col.cellRenderer === 'StatusCellRenderer') return { ...col, cellRenderer: StatusCellRenderer };
						if (col.cellRenderer === 'CurrencyCellRenderer') return { ...col, cellRenderer: CurrencyCellRenderer };
						return { ...col, cellRenderer: DefaultCellRenderer };
					})}
					defaultColDef={defaultColDef}
					modules={[ColumnsToolPanelModule, SetFilterModule, MenuModule, ServerSideRowModelModule, RangeSelectionModule, ClipboardModule]}
					rowHeight={40}
					cellSelection
					headerHeight={40}
					paginationPageSize={20}
					cacheBlockSize={20}
					rowModelType={'serverSide'}
					pagination={true}
					domLayout="normal"
					sideBar={sideBar}
					suppressMovableColumns
					suppressServerSideFullWidthLoadingRow={true}
					noRowsOverlayComponent={() => <span>Record not found.</span>}
				/>
			</div>
			{/* Delete warning dialog using shadcn Dialog */}
			<Dialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
				<DialogContent>
					<DialogHeader>
						<DialogTitle>Delete Invoice</DialogTitle>
					</DialogHeader>
					<div>Are you sure you want to delete this invoice? This action cannot be undone.</div>
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
			<Sheet open={sheetOpen} onOpenChange={handleSheetOpenChange}>
				<SheetContent className="sm:max-w-6xl">
					<SheetHeader>
						<SheetTitle>{editingRow ? 'Edit Invoice' : 'Add Commission'}</SheetTitle>
					</SheetHeader>

					<Dialog open={showUnsavedDialog} onOpenChange={setShowUnsavedDialog}>
						<DialogContent>
							<DialogHeader>
								<DialogTitle>Unsaved Changes</DialogTitle>
							</DialogHeader>
							<div>You have unsaved changes. Are you sure you want to close?</div>
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

					<Dialog open={dialogConfig.isOpen} onOpenChange={(open) => setDialogConfig((prev) => ({ ...prev, isOpen: open }))}>
						<DialogContent>
							<DialogHeader>
								<DialogTitle>{dialogConfig.title}</DialogTitle>
							</DialogHeader>
							<div>{dialogConfig.message}</div>
							<DialogFooter className="flex justify-end gap-2">
								<Button variant="outline" onClick={() => setDialogConfig((prev) => ({ ...prev, isOpen: false }))}>
									Cancel
								</Button>
								<Button
									variant="destructive"
									onClick={() => {
										dialogConfig.onConfirm();
										setDialogConfig((prev) => ({ ...prev, isOpen: false }));
									}}
								>
									Discard & Proceed
								</Button>
							</DialogFooter>
						</DialogContent>
					</Dialog>
					{/* to add a scrollbar for visible the footer */}
					<div className="flex-1 overflow-y-auto overflow-x-hidden pr-2">
						<Form {...form}>
							<form className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-4 sm:gap-x-6 p-2 sm:p-4">
								{!getConfig('acc_id').hidden && (
									<div>
										<FormField
											control={form.control}
											name="acc_id"
											rules={{
												required: getConfig('acc_id').mandatory ? 'Account Name is required' : false,
											}}
											render={({ field }) => (
												<FormItem>
													<FormLabel className="gap-1 font-medium">
														Account Name
														{getConfig('acc_id').mandatory && <span className="text-red-500">*</span>}
													</FormLabel>
													<div style={{ position: 'relative' }}>
														<AsyncTypeahead
															id="acc_id"
															labelKey="account_name"
															options={accountOptions}
															isLoading={isLoading}
															minLength={1}
															placeholder="Search Account Name"
															onSearch={handleAccountSearch}
															onChange={handleAccountChange}
															selected={selectedAccount}
															// Dynamic disabled state
															disabled={getConfig('acc_id').disabled || !!editingRow}
															inputProps={{
																// Use this to match the Account Name style exactly
																className:
																	'flex h-11 w-full rounded-md border border-input bg-background px-4 py-2 text-sm sm:text-base ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-0 disabled:cursor-not-allowed disabled:opacity-50',
																required: getConfig('acc_id').mandatory,
															}}
															menuStyle={{
																zIndex: 1050,
																width: '100%',
																position: 'absolute',
															}}
															renderMenu={renderAccountMenu}
														/>
													</div>
													<div className="min-h-[20px]">
														<FormMessage />
													</div>
												</FormItem>
											)}
										/>
									</div>
								)}
								{!getConfig('invoice_date').hidden && (
									<div>
										<FormField
											control={form.control}
											name="invoice_date"
											rules={{
												required: getConfig('invoice_date').mandatory ? 'Invoice Date is required' : false,
											}}
											render={({ field }) => (
												<FormItem>
													<FormLabel className={`gap-1 font-medium ${getConfig('invoice_date').disabled ? 'opacity-50' : ''}`}>
														Invoice Date
														{getConfig('invoice_date').mandatory && <span className="text-red-500">*</span>}
													</FormLabel>

													<Popover open={open} onOpenChange={setOpen}>
														<PopoverTrigger asChild>
															<Button
																variant="outline"
																disabled={getConfig('invoice_date').disabled}
																className={
																	'w-full justify-start text-left font-normal h-11 text-sm sm:text-base ' +
																	'flex rounded-md border border-input bg-background px-4 py-2 ring-offset-background ' +
																	'focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-0 ' +
																	(field.value ? '' : ' text-muted-foreground') +
																	(getConfig('invoice_date').disabled ? ' opacity-50 cursor-not-allowed' : '')
																}
															>
																<CalendarIcon className="mr-2 h-4 w-4" />
																{field.value ? formatDateTime(field.value + 'T00:00:00') : <span>Pick a date</span>}
															</Button>
														</PopoverTrigger>
														{!getConfig('invoice_date').disabled && (
															<PopoverContent className="w-auto p-0">
																<Calendar
																	mode="single"
																	selected={field.value ? parseISO(field.value) : undefined}
																	onSelect={(date) => {
																		if (!date) {
																			field.onChange('');
																			return;
																		}
																		const formattedDate = format(date, 'yyyy-MM-dd');
																		field.onChange(formattedDate);
																		setOpen(false);
																	}}
																	initialFocus
																/>
															</PopoverContent>
														)}
													</Popover>
													<div className="min-h-[20px]">
														<FormMessage />
													</div>
												</FormItem>
											)}
										/>
									</div>
								)}

								{!getConfig('invoice_no').hidden && (
									<div>
										<FormField
											control={form.control}
											name="invoice_no"
											rules={{
												required: getConfig('invoice_no').mandatory ? 'Invoice Number is required' : false,
												min: {
													value: 0,
													message: 'invoice number cannot be negative',
												},
											}}
											render={({ field }) => (
												<FormItem>
													<FormLabel className="gap-1 font-medium">
														Invoice Number
														{getConfig('invoice_no').mandatory && <span className="text-red-500">*</span>}
													</FormLabel>
													<Input
														{...field}
														type="text"
														disabled={getConfig('invoice_no').disabled || !!editingRow}
														maxLength={256}
														className="flex h-11 w-full rounded-md border border-input bg-background px-4 py-2 text-sm sm:text-base ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-primary disabled:cursor-not-allowed disabled:opacity-50"
														placeholder="Enter invoice number"
														required={getConfig('invoice_no').mandatory}
													/>
													<div className="min-h-[20px]">
														<FormMessage />
													</div>
												</FormItem>
											)}
										/>
									</div>
								)}

								{!getConfig('invoice_status').hidden && (
									<div>
										<FormField
											control={form.control}
											name="invoice_status"
											rules={{
												required: getConfig('invoice_status').mandatory ? 'Invoice Status is required' : false,
											}}
											render={({ field }) => (
												<FormItem>
													<FormLabel className="gap-1 font-medium">
														Invoice Status
														{getConfig('invoice_status').mandatory && <span className="text-red-500">*</span>}
													</FormLabel>
													<Select
														value={field.value}
														onValueChange={field.onChange}
														disabled={getConfig('invoice_status').disabled}
														required={getConfig('invoice_status').mandatory}
													>
														<SelectTrigger className="w-full justify-between h-11 text-sm sm:text-base">
															<SelectValue placeholder="Select status" />
														</SelectTrigger>
														<SelectContent>
															{getConfig('invoice_status').options.map((option) => (
																<SelectItem key={option.value} value={option.value}>
																	{option.label}
																</SelectItem>
															))}
														</SelectContent>
													</Select>
													<div className="min-h-[20px]">
														<FormMessage />
													</div>
												</FormItem>
											)}
										/>
									</div>
								)}

								{!getConfig('commission').hidden && (
									<div>
										<FormField
											control={form.control}
											name="commission"
											rules={{
												required: getConfig('commission').mandatory ? 'Commission is required' : false,
												min: {
													value: 0,
													message: 'commission number cannot be negative',
												},
											}}
											render={({ field }) => (
												<FormItem>
													<FormLabel className="gap-1 font-medium">
														Commission
														{getConfig('commission').mandatory && <span className="text-red-500">*</span>}
													</FormLabel>
													<Input
														{...field}
														type="number"
														step="any"
														disabled={getConfig('commission').disabled}
														className="flex h-11 w-full rounded-md border border-input bg-background px-4 py-2 text-sm sm:text-base ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary disabled:cursor-not-allowed disabled:opacity-50"
														placeholder="Enter commission amount"
														required={getConfig('commission').mandatory}
													/>
													<div className="min-h-[20px]">
														<FormMessage />
													</div>
												</FormItem>
											)}
										/>
									</div>
								)}

								{!getConfig('product').hidden && (
									<div>
										<FormField
											control={form.control}
											name="product"
											rules={{
												required: getConfig('product').mandatory ? 'Product is required' : false,
											}}
											render={({ field }) => (
												<FormItem>
													<FormLabel className="gap-1 font-medium">
														Product
														{getConfig('product').mandatory && <span className="text-red-500">*</span>}
													</FormLabel>
													<Select value={field.value} onValueChange={field.onChange} disabled={getConfig('product').disabled || !!editingRow}>
														<SelectTrigger className="w-full justify-between h-11 text-sm sm:text-base">
															<SelectValue placeholder="Select product" />
														</SelectTrigger>
														<SelectContent>
															{getConfig('product').options.map((option) => (
																<SelectItem key={option.value} value={option.value}>
																	{option.label}
																</SelectItem>
															))}
														</SelectContent>
													</Select>
													<div className="min-h-[20px]">
														<FormMessage />
													</div>
												</FormItem>
											)}
										/>
									</div>
								)}

								{watchedStatus === 'syndicate' && (
									<div className="col-span-1 sm:col-span-2 lg:col-span-3 grid grid-cols-1 gap-0 mt-2 sm:mt-6">
										<div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-2 sm:p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4 mb-4">
											{!getConfig('ticker').hidden && (
												<FormField
													control={form.control}
													name="ticker"
													rules={{
														required: getConfig('ticker').mandatory ? 'Ticker is required' : false,
														min: {
															value: 0,
															message: 'Ticker cannot be negative',
														},
													}}
													render={({ field }) => (
														<FormItem>
															<FormLabel className="gap-1 font-medium text-sm">
																Ticker{getConfig('ticker').mandatory && <span className="text-red-500">*</span>}
															</FormLabel>
															<Input
																{...field}
																disabled={getConfig('ticker').disabled}
																type="text"
																maxLength={32}
																className="h-10 text-sm sm:text-base"
																placeholder="Ticker"
															/>
															<div className="min-h-[20px]">
																<FormMessage />
															</div>
														</FormItem>
													)}
												/>
											)}
											{!getConfig('quantity').hidden && (
												<FormField
													control={form.control}
													name="quantity"
													rules={{
														required: getConfig('quantity').mandatory ? 'Quantity is required' : false,
														min: {
															value: 0,
															message: 'Quantity cannot be negative',
														},
													}}
													render={({ field }) => (
														<FormItem>
															<FormLabel className="gap-1 font-medium text-sm">
																Quantity{getConfig('quantity').mandatory && <span className="text-red-500">*</span>}
															</FormLabel>
															<Input
																{...field}
																disabled={getConfig('quantity').disabled}
																type="number"
																min={0}
																className="h-10 text-sm sm:text-base"
																placeholder="Quantity"
															/>
															<div className="min-h-[20px]">
																<FormMessage />
															</div>
														</FormItem>
													)}
												/>
											)}
											{!getConfig('price').hidden && (
												<FormField
													control={form.control}
													name="price"
													rules={{
														required: getConfig('price').mandatory ? 'Price is required' : false,
														min: {
															value: 0,
															message: 'Price cannot be negative',
														},
													}}
													render={({ field }) => (
														<FormItem>
															<FormLabel className="gap-1 font-medium text-sm">
																Price{getConfig('price').mandatory && <span className="text-red-500">*</span>}
															</FormLabel>
															<Input
																{...field}
																disabled={getConfig('price').disabled}
																type="number"
																min={0}
																step="any"
																className="h-10 text-sm sm:text-base"
																placeholder="Price"
															/>
															<div className="min-h-[20px]">
																<FormMessage />
															</div>
														</FormItem>
													)}
												/>
											)}
											{!getConfig('net_amount').hidden && (
												<FormField
													control={form.control}
													name="net_amount"
													rules={{
														required: getConfig('net_amount').mandatory ? 'Net Amount is required' : false,
														min: {
															value: 0,
															message: 'net Amount cannot be negative',
														},
													}}
													render={({ field }) => (
														<FormItem>
															<FormLabel className="gap-1 font-medium text-sm">
																Net Amount{getConfig('net_amount').mandatory && <span className="text-red-500">*</span>}
															</FormLabel>
															<Input
																{...field}
																disabled={getConfig('net_amount').disabled}
																type="number"
																min={0}
																step="any"
																className="h-10 text-sm sm:text-base"
																placeholder="Net Amount"
															/>
															<div className="min-h-[20px]">
																<FormMessage />
															</div>
														</FormItem>
													)}
												/>
											)}
										</div>

										{/* to get the dynamic fields from dynamicRowFields array */}
										<div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-2 sm:p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4 mb-4">
											{dynamicRowFields.map((item) => (
												<FormField
													key={item.name}
													control={form.control}
													name={item.name}
													rules={getDynamicRules(item)}
													render={({ field }) => (
														<FormItem>
															<FormLabel className="gap-1 font-medium text-sm">
																{item.label}
																{item.mandatory && !item.disabled && <span className="text-red-500">*</span>}
															</FormLabel>

															<Input
																{...field}
																type={item.type === 'number' ? 'number' : 'text'}
																placeholder={item.placeholder}
																readOnly={item.readOnly}
																disabled={item.disabled}
																className={`h-10 text-sm sm:text-base ${
																	item.readOnly || item.disabled ? 'bg-gray-100 dark:bg-gray-700 cursor-not-allowed' : ''
																} ${item.className || ''}`}
																onChange={(e) =>
																	item.readOnly || item.disabled
																		? undefined
																		: item.type === 'number'
																			? field.onChange(e.target.value === '' ? undefined : Number(e.target.value))
																			: field.onChange(e.target.value)
																}
															/>
															<div className="min-h-[20px]">
																<FormMessage />
															</div>
														</FormItem>
													)}
												/>
											))}
										</div>
									</div>
								)}

								<div className="col-span-1 sm:col-span-2 lg:col-span-3 grid grid-cols-1 sm:grid-cols-2 gap-4 sm:gap-6">
									<FormField
										control={form.control}
										rules={{
											required: 'Invoice attachment is required',
										}}
										name="attach"
										render={({ field }) => (
											<FormItem>
												<FormLabel className="gap-1 font-medium">
													Attach Invoice<span className="text-red-500">*</span>
												</FormLabel>
												<div
													className="border-2 border-dashed dark:border-gray-800 border-gray-300 rounded-lg p-3 sm:p-4 flex flex-col items-center justify-center bg-gray-50 hover:bg-gray-100 transition h-[100px] sm:h-[115px] cursor-pointer relative"
													onClick={() => document.getElementById('attach')?.click()}
												>
													<input
														type="file"
														id="attach"
														accept={`.${INVOICE_IMAGE_VARIABLE.value.fileUpload?.fileFormat}` || '.pdf'}
														className="hidden"
														onChange={async (e) => {
															const file = e.target.files?.[0];
															if (!file) return;

															const allowedExt = INVOICE_IMAGE_VARIABLE.value.fileUpload?.fileFormat.toLowerCase();
															const fileExt = file.name.split('.').pop().toLowerCase();
															if (fileExt !== allowedExt) {
																toast.warning(`Only ${allowedExt.toUpperCase()} files are allowed.`);
																return;
															}

															const sizeLimitMB = parseInt(INVOICE_IMAGE_VARIABLE.value.fileUpload?.fileSize || 10);
															const maxSizeInBytes = sizeLimitMB * 1024 * 1024;

															if (file.size > maxSizeInBytes) {
																toast.warning(`File size must be less than ${INVOICE_IMAGE_VARIABLE.value.fileUpload?.fileSize}.`);
																return;
															}

															const reader = new FileReader();
															reader.onloadend = () => {
																const base64Content = reader.result.split(',')[1];
																form.setValue('invoiceFile', file);

																// Pass { shouldValidate: true } to trigger the FormMessage check immediately
																form.setValue('attach', base64Content, { shouldValidate: true });

																form.setValue('att_name', file.name);
																form.setValue('att_size', Math.round(file.size / 1024).toString());
															};

															reader.readAsDataURL(file);
														}}
													/>

													{/* Replace the content inside the border-dashed div with this logic */}
													{form.watch('invoiceFile') || (editingRow && form.watch('att_url')) ? (
														<div className="flex flex-col items-center justify-center w-full space-y-1">
															<div className="flex flex-col items-center text-center">
																<span className="text-xs sm:text-sm font-semibold text-blue-600 truncate max-w-[150px] sm:max-w-[200px]">
																	{form.watch('invoiceFile')?.name || form.watch('att_name') || 'Existing Invoice'}
																</span>
																<span className="text-[10px] sm:text-xs text-gray-500">
																	{form.watch('invoiceFile')
																		? `(${(form.watch('invoiceFile').size / 1024).toFixed(0)} KB)`
																		: form.watch('att_size')
																			? `(${form.watch('att_size')} KB)`
																			: ''}
																</span>
															</div>

															<div className="flex gap-2 w-full max-w-[200px]">
																{editingRow && form.watch('att_url') && !form.watch('invoiceFile') && (
																
																	<Button
																		type="button"
																		variant="outline"
																		size="sm"
																		className="h-7 flex-1 text-[10px] sm:text-xs"
																		onClick={(e) => {
																			e.stopPropagation();
																			window.open(form.watch('att_url'), '_blank');
																		}}
																	>
																		<Download className="mr-1" size={12} /> Download
																	</Button>
																)}
																<Button
																	type="button"
																	variant="destructive"
																	size="sm"
																	className="h-7 flex-1 text-[10px] sm:text-xs"
																	onClick={(e) => {
																		e.stopPropagation();
																		form.setValue('invoiceFile', '');
																		form.setValue('attach', '', { shouldValidate: true });
																		form.setValue('att_name', '');
																		form.setValue('att_size', '');
																		form.setValue('att_url', '');
																		if (document.getElementById('attach')) {
																			document.getElementById('attach').value = '';
																		}
																	}}
																>
																	Remove
																</Button>
															</div>
														</div>
													) : (
														<div className="flex flex-col items-center text-center">
															<span className="text-gray-400 text-xs sm:text-sm">Drag & drop or click to upload</span>
															<span className="text-[9px] sm:text-[10px] text-gray-400 uppercase mt-1">
																{INVOICE_IMAGE_VARIABLE.value.fileUpload?.fileFormat} Max {INVOICE_IMAGE_VARIABLE.value.fileUpload?.fileSize}
															</span>
														</div>
													)}
												</div>
												<div className="min-h-[20px]">
													<FormMessage />
												</div>
											</FormItem>
										)}
									/>

									{!getConfig('comments').hidden && (
										<FormField
											control={form.control}
											name="comments"
											rules={{
												required: getConfig('comments').mandatory ? 'Notes are required' : false,
											}}
											render={({ field }) => (
												<FormItem>
													<FormLabel className="gap-1 font-medium">
														Notes
														{getConfig('comments').mandatory && <span className="text-red-500">*</span>}
													</FormLabel>
													<textarea
														{...field}
														id="notes"
														rows={4}
														disabled={getConfig('comments').disabled}
														className="border rounded w-full min-w-0 p-2 text-sm sm:text-base focus:outline-none focus:ring-1 focus:ring-primary disabled:opacity-50 disabled:bg-gray-100"
														placeholder="Enter notes here..."
														required={getConfig('comments').mandatory}
													/>
													<div className="min-h-[20px]">
														<FormMessage />
													</div>
												</FormItem>
											)}
										/>
									)}
								</div>
							</form>
						</Form>

						{/* Only visible the coverage details once the Account name selected */}
						{selectedAccount.length > 0 && (
							<div className="p-2 sm:p-4">
								<h3 className="text-base sm:text-lg font-semibold mb-2">Coverage Details</h3>

								<div className="mb-4 flex flex-col sm:flex-row gap-2 items-stretch sm:items-end max-w-full sm:max-w-[500px]">
									<div style={{ position: 'relative' }} className="flex-1 w-full sm:flex-[2]">
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
												className: 'h-10 w-full border rounded-md px-3 py-2 text-sm sm:text-base',
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
												className="dark:bg-gray-800 bg-gray-100 border"
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
										<div className="flex items-center gap-2">
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
												className="input input-bordered border rounded px-3 py-2 text-sm sm:text-base w-20 sm:w-24 h-10"
												disabled={!selectedEmployee}
											/>
											<Button
												type="button"
												variant="default"
												className="h-10 text-sm sm:text-base whitespace-nowrap"
												onClick={handleAddCoverage}
												disabled={!selectedEmployee || percentage === '' || (percentage !== '' && Number(percentage) > 100)}
											>
												Add
											</Button>
										</div>
									)}
								</div>
								{coverageRows.length > 0 && (
									<div className="w-full border rounded-md overflow-hidden" style={{ height: 220, minHeight: 120, overflow: 'auto' }}>
										<table className="w-full text-sm text-left border-collapse">
											
											<thead className={`${IS_DARK.value ? 'bg-[#2d3436] text-white' : 'bg-gray-100'} sticky top-0 z-10`}>
												<tr style={{ height: 34 }}>
													{coverageColumns.map((col) => (
														<th key={col.headerName} className="px-4 font-semibold border-b text-xs uppercase tracking-wider">
															{col.headerName}
														</th>
													))}
												</tr>
											</thead>
=
											<tbody className={IS_DARK.value ? 'bg-[#181d1f] text-gray-300' : 'bg-white text-gray-700'}>
												{coverageRows.map((row, index) => (
													<tr key={row.id || index} style={{ height: 34 }} className="border-b hover:bg-gray-50/5 transition-colors">
														<td className="px-4 truncate">{row.display_name}</td>
														<td className="px-4 truncate">{row.corp_title}</td>
														<td className="px-4 truncate">{row.email}</td>
														<td className="px-4">
															<input
																type="number"
																value={row.percentage || ''}
																onChange={(e) => {
																	const val = e.target.value;
																	const updatedRows = coverageRows.map((r) => (r.emp_id === row.emp_id ? { ...r, percentage: val } : r));
																	setCoverageRows(updatedRows); 
																}}
																className="w-full bg-transparent border border-gray-200 focus:ring-1 focus:ring-blue-500 rounded px-1"
																min="0"
																max="100"
															/>
														</td>

														<td className="px-4">
															<div className="flex justify-center gap-2">
																<button
																	type="button"
																	className="cursor-pointer text-gray-400 hover:text-red-500 transition-colors"
																	title="Delete"
																	onClick={() => handleDeleteCoverage(row)}
																>
																	<Trash2 size={18} strokeWidth={1.5} />
																</button>
															</div>
														</td>
													</tr>
												))}
											</tbody>
										</table>
									</div>
								)}
							</div>
						)}
					</div>
					<SheetFooter className="flex flex-row justify-end border-t pt-4 mt-4">
						<Button
							className={`${!form.formState.isValid ? 'opacity-50 cursor-not-allowed' : ''}`}
							type="submit"
							onClick={form.handleSubmit(editingRow ? handleUpdateSubmit : handleFormSubmit)}
						>
							{editingRow ? 'Update' : 'Save'}
						</Button>
						<Button variant="outline" type="button" onClick={() => handleSheetOpenChange(false)}>
							Cancel
						</Button>
					</SheetFooter>
				</SheetContent>
			</Sheet>
		</div>
	);
}
----------------------------------------------------------------------------------------
	
