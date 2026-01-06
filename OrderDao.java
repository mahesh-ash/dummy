import React, { useRef, useState, useMemo, useEffect, useCallback } from 'react';
import { AsyncTypeahead } from 'react-bootstrap-typeahead';
import { AgGridReact } from '@ag-grid-community/react';
import { ColumnsToolPanelModule } from '@ag-grid-enterprise/column-tool-panel';
import { SetFilterModule } from '@ag-grid-enterprise/set-filter';
import { MenuModule } from '@ag-grid-enterprise/menu';
import { Pencil, Trash2, Plus, Download, CircleDot, CheckCircle2, Clock, RefreshCcwIcon } from 'lucide-react';
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
import { Input } from '@/components/ui/input';
import { toast } from 'sonner';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { AUTH_TOKEN, INVOICE_DATE_RANGE, INVOICE_IMAGE_VARIABLE, INVOICE_TABLE_SETTING, IS_DARK } from '@/store/signals';
import { useSignals } from '@preact/signals-react/runtime';
import { DateRangePicker } from '../common/DateRangePicker';
import { useMutation } from 'react-query';
import { getAllCommission, getInvoiceDetails, getTableFields, saveInvoice, deleteInvoice, saveTableFields, updateInvoice } from '@/utils/service';
import z from 'zod';
import { ServerSideRowModelModule } from '@ag-grid-enterprise/server-side-row-model';
import { format } from 'date-fns';
import { convertDate, getDateRange } from '@/common/helpers/DateTimeFormat';
import { createFormData, formatDate } from '@/utils/common';
import { RangeSelectionModule } from '@ag-grid-enterprise/range-selection';
import { ClipboardModule } from '@ag-grid-enterprise/clipboard';
import { Textarea } from '@/components/ui/textarea';
import ButtonGroupDropdown from '@/common/button-group/button';
import { OverlayLoader } from '../common/OverlayLoader';

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

const dynamicRowFields = Array.isArray(INVOICE_IMAGE_VARIABLE.value?.formConfig?.extraFormFields)
	? INVOICE_IMAGE_VARIABLE.value.formConfig.extraFormFields.map((f, index) => ({
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
		}))
	: [];

const dynamicRowDefaults = Array.isArray(dynamicRowFields)
	? dynamicRowFields.reduce((acc, field) => {
			acc[field.name] = field.defaultValue ?? (field.type === 'number' ? undefined : '');
			return acc;
		}, {})
	: {};

// const getDynamicRules = (field) => {
// 	const rules = {};

// 	if (field.mandatory && !field.readOnly && !field.disabled) {
// 		rules.required = `${field.label} is required`;
// 	}

// 	if (field.pattern && !field.disabled) {
// 		rules.pattern = {
// 			value: field.pattern,
// 			message: field.patternMessage || 'Invalid format',
// 		};
// 	}

// 	return rules;
// };

function CurrencyCellRenderer(params) {
	const value = params.value;
	const formattedValue = new Intl.NumberFormat('en-US', {
		style: 'currency',
		currency: 'USD',
	}).format(value);
	return <div title={formattedValue}>{formattedValue}</div>;
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
	// if (status === 'pending') {
	// 	color = isDark ? 'bg-blue-900 text-blue-200' : 'bg-blue-100 text-blue-800';
	// 	icon = <CircleDot size={16} className={isDark ? 'mr-1 text-blue-300' : 'mr-1 text-blue-600'} />;
	// }
	// if (status === 'active') {
	// 	color = isDark ? 'bg-cyan-900 text-cyan-200' : 'bg-cyan-100 text-cyan-800';
	// 	icon = <CheckCircle2 size={16} className={isDark ? 'mr-1 text-cyan-300' : 'mr-1 text-cyan-600'} />;
	// }
	return (
		<span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold gap-1 ${color}`} style={{ minWidth: 80 }}>
			{icon}
			{status?.replace(/^./, (char) => char.toUpperCase())}
		</span>
	);
}

export default function ManageInvoiceTable(props) {
	useSignals();
	const saveInvoiceMutation = useMutation({
		mutationFn: saveInvoice,
	});

	const updateInvoiceMutation = useMutation({
		mutationFn: updateInvoice,
	});

	const getInvoiceDetailsMutation = useMutation({
		mutationFn: getInvoiceDetails,
	});

	const deleteInvoiceMutation = useMutation({
		mutationFn: deleteInvoice,
	});

	const gridRef = useRef();
	// Loading states
	const [includeEmployee, setIncludeEmployee] = useState(false);

	// Typeahead for Account Name
	const [accountOptions, setAccountOptions] = useState([]);
	const [accountQuery, setAccountQuery] = useState('');
	const [rangeStartDate, setRangeStartDate] = useState('');
	const [rangeEndDate, setRangeEndDate] = useState('');
	const [selectedAccount, setSelectedAccount] = useState([]);
	const [sheetOpen, setSheetOpen] = useState(false);

	const [tickerOptions, setTickerOptions] = useState([]);
	const [isTickerLoading, setIsTickerLoading] = useState(false);
	const [selectedTicker, setSelectedTicker] = useState([]);
	const [tickerQuery, setTickerQuery] = useState('');

	// Grid and sheet states
	const [showUnsavedDialog, setShowUnsavedDialog] = useState(false);
	const [editingRow, setEditingRow] = useState(null);
	const [showDeleteDialog, setShowDeleteDialog] = useState(false);
	const [rowToDelete, setRowToDelete] = useState(null);
	const [showCoverageDeleteDialog, setShowCoverageDeleteDialog] = useState(false);
	const [coverageRowToDelete, setCoverageRowToDelete] = useState(null);

	const [open, setOpen] = useState(false);

	const [dialogConfig, setDialogConfig] = useState({
		isOpen: false,
		title: '',
		message: '',
		onConfirm: () => {},
	});

	// excell downloading
	const [excelLoading, setExcelLoading] = useState(false);
	const [columnDefs, setColumnDefs] = useState([]);
	const [percentage, setPercentage] = useState('');
	const [coverageRows, setCoverageRows] = useState([]);
	const [coverageError, setCoverageError] = useState('');
	const [percentageInputError, setPercentageInputError] = useState('');
	const [height, setGridHeight] = useState('75vh');
	const [selectedEmployee, setSelectedEmployee] = useState([]);
	const [lastRefresh, setLastRefresh] = useState(null);
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

	const base64ToBlob = (base64, type = 'application/pdf') => {
		const binStr = atob(base64);
		const len = binStr.length;
		const arr = new Uint8Array(len);
		for (let i = 0; i < len; i++) {
			arr[i] = binStr.charCodeAt(i);
		}
		return new Blob([arr], { type });
	};

	useEffect(() => {
		if (percentageInputError != '') {
			toast.error(percentageInputError);
		}
	}, [percentageInputError]);

	useEffect(() => {
		if (props && props.height) {
			const gridElement = document.querySelector('.grid-container.invoice-table');
			if (gridElement) {
				const { offsetTop } = gridElement;
				setGridHeight(`${props.height - offsetTop - 20}px`);
			}
		}
	}, [props]);

	useEffect(() => {
		if (updateInvoiceMutation.data && updateInvoiceMutation.data.status === 1) {
			setSheetOpen(false);
			setEditingRow(null);
			form.reset(defaultValues);
			setCoverageRows([]);

			// Refresh the grid with updated data
			if (gridRef.current?.api) {
				gridRef.current.api.refreshServerSide({ purge: true });
				// Clear selections
				gridRef.current.api.deselectAll();
			}
		} else if (updateInvoiceMutation.data && updateInvoiceMutation.data.status === 0) {
			toast.error(updateInvoiceMutation.data.message || 'Update failed');
		}
	}, [updateInvoiceMutation.isSuccess]);

	useEffect(() => {
		if (saveInvoiceMutation.data && saveInvoiceMutation.data.status === 1) {
			setSheetOpen(false);
			form.reset(defaultValues);
			setCoverageRows([]);

			// Refresh the grid after adding new invoice
			if (gridRef.current?.api) {
				gridRef.current.api.refreshServerSide({ purge: true });
				gridRef.current.api.deselectAll();
			}
		} else if (saveInvoiceMutation.data && saveInvoiceMutation.data.status === 0) {
			toast.error(saveInvoiceMutation.data.message || 'Save failed');
		}
	}, [saveInvoiceMutation.isSuccess]);

	useEffect(() => {
		if (deleteInvoiceMutation.data && deleteInvoiceMutation.data.status === 1) {
			setShowDeleteDialog(false);
			setRowToDelete(null);
			// Refresh the grid
			const datasource = createDataSource();
			gridRef.current?.api?.refreshServerSide({ purge: false });
			gridRef.current?.api?.setGridOption('serverSideDatasource', datasource);
		} else {
			setShowDeleteDialog(false);
		}
	}, [deleteInvoiceMutation.isSuccess]);

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

	async function handleTickerSearch(query) {
		setTickerQuery(query);
		setIsTickerLoading(true);

		// Ensure account is selected if needed, similar to employee search

		try {
			// Updated URL for ticker search behavior
			const response = await httpClient.get(`Search/GetSearchText?searchText=${encodeURIComponent(query)}&type=tickerQuery`);
			setTickerOptions(response.data.data);
		} catch (error) {
			console.error('Error searching Tickers:', error);
		} finally {
			setIsTickerLoading(false);
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
		return `${mm}/${dd}/${yyyy}`;
	}

	const downloadInvoice = async (data) => {
		try {
			// toast.success('Downloading...');
			const response = await fetch(`${import.meta.env.VITE_API_URL}Attachment/Download?attachment_id=${encodeURIComponent(data.att_id)}`, {
				method: 'GET',
				headers: {
					auth_token: encodeURIComponent(AUTH_TOKEN.value),
					'Content-Type': 'application/x-www-form-urlencoded',
				},
			});

			if (!response.ok) {
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
			a.download = headerFileName || `invoice_${data.invoice_no}.pdf`;
			document.body.appendChild(a);
			a.click();
			a.remove();
			toast.success('Downloaded');
			window.URL.revokeObjectURL(url);
		} catch (error) {
			console.error('Download failed:', error);
		}
	};

	function DefaultCellRenderer(props) {
		const col = props.colDef;

		// Custom Invoice column renderer
		if (col.field === 'invoice_no') {
			return (
				<div className={col.className + ' relative flex items-center gap-2'} style={col.cellStyle} title={props.value}>
					<TooltipProvider>
						<Tooltip>
							<TooltipTrigger asChild>
								<button
									type="button"
									className="text-blue-500 hover:text-blue-700 transition-colors flex-shrink-0"
									style={{ background: 'none', border: 'none', cursor: 'pointer' }}
									onClick={(e) => {
										e.stopPropagation();
										if (props.context?.downloadInvoice) {
											props.context.downloadInvoice(props.data);
										} else {
											downloadInvoice(props.data);
										}
									}}
								>
									<Download size={16} />
								</button>
							</TooltipTrigger>
							<TooltipContent>Download Invoice</TooltipContent>
						</Tooltip>
					</TooltipProvider>
					<span className="truncate">{props.value}</span>
				</div>
			);
		}

		return (
			<div className={col.className} style={col.cellStyle} title={props.value}>
				{props.value == 'null' || props.value == null || !props.value ? '-' : props.value}
			</div>
		);
	}

	function formatDateInt(date) {
		if (!date) return '';
		const d = new Date(date);
		if (isNaN(d)) return '';
		const mm = String(d.getMonth() + 1).padStart(2, '0');
		const dd = String(d.getDate()).padStart(2, '0');
		const yyyy = d.getFullYear();
		return `${mm}/${dd}/${yyyy}`;
	}

	// Format date as yyyy-MM-dd
	// function formatDate(date) {
	// 	if (!date) return '';
	// 	const d = new Date(date);
	// 	// console.log(d.toISOString().slice(0, 10));
	// 	return d.toISOString().slice(0, 10);
	// }
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
			toast.success('Downloaded');
			window.URL.revokeObjectURL(url);
			setExcelLoading(false);
		} catch (error) {
			toast.error('Download Failed');
			setExcelLoading(false);
			console.error('Download failed:', error);
		}
	};

	function excelDownloadHandler(includeOverride) {
		const columnState = gridRef.current?.api?.getColumnState();
		const sortModel = columnState
			.filter((col) => col.sort != null) // Filter for columns with sorting applied
			.map((col) => ({
				colId: col.colId,
				sort: col.sort,
			}));
		const clientTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
		const include = typeof includeOverride === 'boolean' ? includeOverride : includeEmployee === true;
		const obj = {
			start: 0,
			length: 0,
			start_date: rangeStartDate || '',
			end_date: rangeEndDate || '',
			filters: '',
			order: '',
			include_employee: include,
		};

		// Include employee splits when requested
		if (include) {
			const empSplits = coverageRows.map((r) => ({ emp_id: String(r.emp_id), split: parseFloat(r.percentage || 0).toFixed(5) })).filter((s) => s.emp_id);
			obj.emp_splits = empSplits.length ? JSON.stringify(empSplits) : '';
		} else {
			obj.emp_splits = '';
		}

		downloadEBBExcel('getInvoiceData', obj);
	}

	function dateRangeHandler(value) {
		// value: { range: { from, to }, rangeCompare }
		const from = value?.range?.from ? formatDate(value.range.from, false) : '';
		const to = value?.range?.to ? formatDate(value.range.to, false) : '';
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
			const isAlreadyAdded = coverageRows.some((row) => row.emp_id === employeeData.emp_id);

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

	// Helper to remove width from column state
	function stripWidthFromColumnState(state) {
		if (!Array.isArray(state)) return state;
		return state.map((col) => {
			const { width, ...rest } = col;
			return rest;
		});
	}

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
					formDataObj.filters = JSON.stringify(requestParams.request.filterModel) || '';
				}

				const formData = createFormData(formDataObj);
				handleApiResponse(getAllCommission(formData));
			},
		};
	}

	useEffect(() => {
		setColumnDefs(INVOICE_TABLE_SETTING.value);
	}, [INVOICE_TABLE_SETTING.value]);

	useEffect(() => {
		if (INVOICE_DATE_RANGE.value) {
			setRangeStartDate(formatDate(getDateRange(INVOICE_DATE_RANGE.value).start, false));
			setRangeEndDate(formatDate(getDateRange(INVOICE_DATE_RANGE.value).end, false));
		}
	}, [INVOICE_DATE_RANGE.value]);

	useEffect(() => {
		getTableFields({ tblid: JSON.stringify(['invoice_table']) }).then((data) => {
			if (data && data['invoice_table']) {
				let columnsState = JSON.parse(data['invoice_table'].column_def);
				if (columnsState.length > 0) {
					columnsState = columnsState[0].columnState;
				}
				if (gridRef.current) {
					gridRef.current?.api?.applyColumnState({
						state: stripWidthFromColumnState(columnsState),
					});
				}
			} else {
				if (gridRef.current) {
					gridRef.current?.api?.applyColumnState({
						state: stripWidthFromColumnState([]),
					});
				}
			}
			setTimeout(() => {
				const datasource = createDataSource(rangeStartDate, rangeEndDate); // Clear search text for tab/dropdown changes
				gridRef.current?.api?.setGridOption('serverSideDatasource', datasource);
			}, 1000);
		});
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

					// Map API employee objects to the coverage row shape and set default percentages
					const mapped = employees.map((emp) => ({
						emp_id: (emp.emp_id || emp.id || '').toString(),
						display_name: emp.display_name || emp.emp_name || emp.name || '',
						corp_title: emp.corp_title || emp.role_name || '',
						email: emp.email || emp.emp_email || '',
						percentage: '',
					}));

					// Default allocation logic: 1 employee -> 100, 2 employees -> 50/50, >2 -> leave blank for manual entry
					if (mapped.length === 1) {
						mapped[0].percentage = 100;
					} else if (mapped.length === 2) {
						mapped[0].percentage = 50;
						mapped[1].percentage = 50;
					}

					setCoverageRows(mapped);

					const splitsArray = mapped.map((emp) => ({
						emp_id: emp.emp_id.toString(),
						split: emp.percentage === '' ? '' : String(emp.percentage),
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

	// Ensure these state variables are defined using useState:
	// const [tickerQuery, setTickerQuery] = useState('');
	// const [tickerOptions, setTickerOptions] = useState([]);

	function renderTickerMenu(results, menuProps) {
		// Hide menu if no query and no existing options
		if (tickerQuery.length === 0 && tickerOptions.length === 0) {
			return null;
		}

		// Destructure to avoid React warnings about internal props
		const { paginationText, newSelectionPrefix, renderMenuItemChildren, ...validMenuProps } = menuProps;

		const items = results.map((result, index) => {
			if (result.paginationOption) {
				return (
					<MenuItem className="default-menu-item rounded !p-0 justify-center gap-1 text-xm" href="javascript:void(0);" key={index} option={result}>
						<div>Show More</div>
						<ChevronDownCircle className="size-4 stroke-[#aaa]" />
					</MenuItem>
				);
			} else {
				return (
					<MenuItem className="default-menu-item" href="javascript:void(0);" key={index} option={result}>
						{/* Display Symbol and Description for clarity */}
						<div className="flex flex-col">
							<span className="font-bold">{result.ticker_symbol}</span>
							<span className="text-xs text-gray-500">{result.ticker_description}</span>
						</div>
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

	const handleTickerChange = (selected) => {
		// selected is always an array in react-bootstrap-typeahead
		setSelectedTicker(selected);

		if (selected && selected.length > 0) {
			const option = selected[0];
			// Set the form values (adjust field names based on your Zod/Form schema)
			// populate the `ticker` field so required validation passes
			form.setValue('ticker', option.ticker_symbol, { shouldValidate: true });
			// keep description in case other logic/readers expect it
			// form.setValue('ticker_description', option.ticker_description);
		} else {
			// Handle clearing the selection
			form.setValue('ticker', '', { shouldValidate: true });
			form.setValue('ticker_description', '');
		}
	};

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

	function saveGridSettings() {
		const columnState = gridRef.current?.api?.getColumnState();
		const formData = new FormData();

		const settings = {
			columnState,
		};
		formData.append('tblid', 'invoice_table');
		formData.append('sorting', '');
		formData.append('settings', JSON.stringify([settings]));
		saveTableFields(formData);
	}

	function handleDeleteCoverage(row) {
		setCoverageRowToDelete(row);
		setShowCoverageDeleteDialog(true);
	}

	function handleCoverageDeleteConfirm() {
		setCoverageRows((prev) => prev.filter((r) => r.emp_id !== coverageRowToDelete.emp_id));
		setShowCoverageDeleteDialog(false);
		setCoverageRowToDelete(null);
	}

	function handleCoverageDeleteCancel() {
		setShowCoverageDeleteDialog(false);
		setCoverageRowToDelete(null);
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
			emp_id: employeeData.emp_id || employeeData.id,
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

	// const EmpSplitSchema = z.object({
	// 	emp_id: z.string().nonempty(),
	// 	split: z.string().nonempty(),
	// });

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
		att_id: '',
		att_name: '',
		att_size: '',
		attach: '',
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

	useEffect(() => {
		if (getInvoiceDetailsMutation.data && getInvoiceDetailsMutation.isSuccess) {
			const rowData = getInvoiceDetailsMutation.data;
			setEditingRow(rowData);
			let empSplitsArray = [];
			if (rowData.emp_splits) {
				try {
					empSplitsArray = typeof rowData.emp_splits === 'string' ? JSON.parse(rowData.emp_splits) : rowData.emp_splits;
				} catch (e) {
					console.error('Error parsing emp_splits:', e);
				}
			}

			// This populates your table immediately
			const mappedRows = empSplitsArray.map((emp) => ({
				...emp,
				emp_id: emp.emp_id,
				display_name: emp.emp_name,
				corp_title: emp.role_name,
				email: emp.emp_email,
				percentage: emp.percentage,
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
				product: rowData.product?.toLowerCase() || '',
				invoice_date: rowData.invoice_date || '',
				invoice_no: rowData.invoice_no || '',
				invoice_status: rowData.invoice_status?.toLowerCase() || '',
				commission: parseFloat(rowData.commission).toFixed(2) || '',
				ticker: rowData.ticker || '',
				quantity: rowData.quantity || '',
				price: parseFloat(rowData.price).toFixed(2) || '',
				payOut: rowData.payOut !== undefined ? rowData.payOut : true,
				net_amount: parseFloat(rowData.net_amount).toFixed(2) || '',
				comments: rowData.comments || '',
				att_id: rowData.att_id || '',
				att_url: rowData.att_url,
				att_name: rowData.att_name,
				emp_splits: empSplitsArray,
				...dynamicRowFields.reduce((acc, field) => {
					if (rowData[field.name] !== undefined) {
						acc[field.name] = rowData[field.name];
					}
					return acc;
				}, {}),
			});

			// Ensure the typeahead shows the current ticker when editing
			if (rowData.ticker) {
				const tickerOption = {
					ticker_symbol: rowData.ticker,
					ticker_description: rowData.ticker_description || rowData.ticker,
				};
				setSelectedTicker([tickerOption]);
				setTickerOptions((prev) => {
					try {
						if (!prev || !Array.isArray(prev)) return [tickerOption];
						const exists = prev.some((o) => o && o.ticker_symbol === tickerOption.ticker_symbol);
						return exists ? prev : [tickerOption, ...prev];
					} catch (e) {
						return [tickerOption];
					}
				});
			}

			setSheetOpen(true);
		}
	}, [getInvoiceDetailsMutation.isSuccess, getInvoiceDetailsMutation.data]);

	// function cellEditingStopped(e) {
	// 	if (e.colDef.field === 'percentage') {
	// 		let attemptedValue = e.newValue !== undefined ? Number(e.newValue) : Number(e.value);
	// 		if (!isNaN(attemptedValue) && attemptedValue > 100) {
	// 			toast.error('Percentage cannot be more than 100.');
	// 		}
	// 	}
	// }

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
								data-clickid={`Action: Invoice Manager - Edit`}
								className="text-gray-400"
								style={{ background: 'none', border: 'none', cursor: 'pointer' }}
								onClick={() => {
									getInvoiceDetailsMutation.mutate({ invoice_no: row.invoice_no, acc_id: row.acc_id });
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
								data-clickid={`Action: Invoice Manager - Delete`}
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
			setTickerOptions([]);
			setSelectedTicker([]);
			setSelectedEmployee([]);
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
			deleteInvoiceMutation.mutate({ invoice_no: rowToDelete.invoice_no, acc_id: rowToDelete.acc_id });
		}
	}

	function handleDeleteCancel() {
		setShowDeleteDialog(false);
		setRowToDelete(null);
	}

	// to remove the undefined or NaN with empty values
	const sanitizeData = (obj) => {
		return Object.keys(obj).reduce(
			(acc, key) => {
				const value = obj[key];

				// Nested objects / arrays: recurse
				if (typeof value === 'object' && value !== null) {
					acc[key] = sanitizeData(value);
					return acc;
				}

				// Replace undefined, NaN, 'NaN', 'null' with empty string
				if (
					value === undefined ||
					(typeof value === 'number' && Number.isNaN(value)) ||
					(typeof value === 'string' && value.trim().toLowerCase() === 'nan') ||
					(typeof value === 'string' && value.trim() === 'null')
				) {
					acc[key] = '';
				} else {
					acc[key] = value;
				}

				return acc;
			},
			Array.isArray(obj) ? [] : {}
		);
	};

	async function handleUpdateSubmit(data) {
		const cleanData = sanitizeData(data);
		cleanData.invoice_date = cleanData.invoice_date ? formatDateInt(cleanData.invoice_date) : '';

		const totalPercentage = coverageRows.reduce((sum, row) => {
			return sum + (Number(row.percentage) || 0);
		}, 0);

		// Strict Validation: Must be exactly 100
		if (Math.abs(totalPercentage - 100) > 0.00001) {
			toast.error(`Total allocation must be exactly 100%. Current total: ${totalPercentage.toFixed(2)}%`);
			return;
		}

		// const validEmployees = coverageRows.filter((row) => row.emp_id && (parseFloat(row.percentage) || 0) > 0);

		const filteredSplits = coverageRows
			.filter((row) => row.emp_id && (parseFloat(row.percentage) || 0) > 0)
			.map((emp) => ({
				emp_id: String(emp.emp_id),
				split: parseFloat(emp.percentage).toFixed(2),
			}));

		// const updatedData = {
		// 	...cleanData,
		// 	emp_splits: JSON.stringify(filteredSplits),
		// };

		const updatedData = {
			...cleanData,
			emp_splits: JSON.stringify(filteredSplits),
			invoice_no: editingRow?.invoice_no || cleanData.invoice_no,
		};

		{
			const candidate = form.getValues().attach || updatedData.attach || '';
			let rawBase64 = '';
			if (candidate) {
				if (typeof candidate === 'string') {
					rawBase64 = candidate;
				} else {
					rawBase64 = '';
				}
			}

			if (rawBase64) {
				updatedData.attach = rawBase64;
				updatedData.att_name = form.getValues().att_name || updatedData.att_name || '';
				updatedData.att_size = form.getValues().att_size || updatedData.att_size || '';
			} else {
				updatedData.attach = '';
			}
		}

		// updatedData.att_id = editingRow.att_id;

		delete updatedData.invoiceFile;
		delete updatedData.att_url;
		// delete updatedData.ticker;
		// delete updatedData.quantity;
		// delete updatedData.price;
		// delete updatedData.net_amount;

		const formData = createFormData(updatedData);
		updateInvoiceMutation.mutate(formData);
	}

	const getRowId = useCallback((params) => params.data.emp_id, []);
	function handleFormSubmit(data) {
		const cleanData = sanitizeData(data);

		// Calculate total using Number() to handle strings from inputs
		// Validate coverage totals (strict 100)
		const totalPercentage = coverageRows.reduce((sum, row) => sum + (Number(row.percentage) || 0), 0);
		if (Math.abs(totalPercentage - 100) > 0.00001) {
			toast.error(`Total allocation must be exactly 100%. Current total: ${totalPercentage.toFixed(2)}%`);
			return;
		}

		// Build emp_splits
		const filteredSplits = coverageRows
			.filter((row) => row.emp_id && (parseFloat(row.percentage) || 0) > 0)
			.map((emp) => ({
				emp_id: String(emp.emp_id),
				split: parseFloat(emp.percentage).toFixed(2),
			}));

		const updatedData = {
			...cleanData,
			emp_splits: JSON.stringify(filteredSplits),
		};

		// Attachment: ensure raw base64 (no data: prefix) is sent
		{
			const rawCandidate = form.getValues().attach || updatedData.attach || '';
			const rawBase64 = rawCandidate && typeof rawCandidate === 'string' ? (rawCandidate.startsWith('data:') ? rawCandidate.split(',')[1] : rawCandidate) : '';
			updatedData.attach = rawBase64 || '';
			updatedData.att_name = form.getValues().att_name || updatedData.att_name || '';
			updatedData.att_size = form.getValues().att_size || updatedData.att_size || '';
		}

		// Decide allowed keys based on product/invoice_status
		const prod = (form.getValues().product || updatedData.product || '').toString().toLowerCase();

		const invoiceAllowedCreate = [
			'acc_id',
			'invoice_date',
			'invoice_no',
			'invoice_status',
			'commission',
			'product',
			'payOut',
			'comments',
			'attach',
			'att_name',
			'att_id',
			'att_size',
			'emp_splits',
		];

		const syndicateAllowedCreate = Array.from(new Set([...invoiceAllowedCreate, 'ticker', 'quantity', 'price', 'net_amount']));

		// dynamic mandatory fields (only include mandatory, not readOnly/disabled)
		const dynamicMandatoryFields = (Array.isArray(dynamicRowFields) ? dynamicRowFields : []).filter((f) => f.mandatory && !f.readOnly && !f.disabled).map((f) => f.name);

		// Build final payload keys
		let allowedKeys = prod === 'syndicate' ? new Set(syndicateAllowedCreate) : new Set(invoiceAllowedCreate);

		if (prod === 'syndicate') {
			dynamicMandatoryFields.forEach((k) => allowedKeys.add(k));
		}

		// Compose final payload only with allowed keys (fetch values from form or updatedData)
		const finalPayload = {};
		allowedKeys.forEach((key) => {
			const formVal = form.getValues()[key];
			const val = formVal !== undefined ? formVal : updatedData[key];
			// treat undefined as omitted
			if (val !== undefined) finalPayload[key] = val;
		});

		// Ensure emp_splits present even if empty
		// Prefer the computed splits from updatedData to avoid stale form values
		if (updatedData && updatedData.emp_splits) {
			finalPayload.emp_splits = updatedData.emp_splits;
		} else if (!finalPayload.emp_splits) {
			finalPayload.emp_splits = JSON.stringify([]);
		}
		finalPayload.invoice_date = finalPayload.invoice_date ? formatDateInt(finalPayload.invoice_date) : '';
		const formData = createFormData(finalPayload);
		saveInvoiceMutation.mutate(formData);
	}

	// const onCellValueChanged = (params) => {
	// 	const updatedRows = [...coverageRows];
	// 	const index = updatedRows.findIndex((row) => row.emp_id === params.data.emp_id);
	// 	if (index > -1) {
	// 		updatedRows[index] = params.data;
	// 		setCoverageRows(updatedRows);
	// 	}
	// };

	function handleRefresh() {
		if (gridRef.current) {
			gridRef.current?.api?.refreshServerSide({ purge: true });
		}
	}


	
	
const fields = INVOICE_IMAGE_VARIABLE.value.formConfig?.formFields || [];

const fieldConfigs = fields.reduce((acc, field, idx) => {
    acc[field.name] = {
        mandatory: field.mandatory ?? true,
        disabled: field.disabled ?? false,
        hidden: field.hidden ?? false,
        options: field.options || [],
        order: idx,
    };
    return acc;
}, {});

// Map the original 'fields' array instead of the 'fieldConfigs' object
const fieldOrder = fields.map((f) => f.name);


	const isFutureDate = (day) => {
		const today = new Date();
		// Set hours to 00:00:00:000 to compare dates only
		today.setHours(0, 0, 0, 0);
		return day > today;
	};

	// common function to get the configurations from dynamically
	const getConfig = (fieldName) => {
		if (fieldConfigs[fieldName]) return fieldConfigs[fieldName];
		return { mandatory: true, disabled: false, hidden: false, options: [], order: fieldOrder.indexOf(fieldName) >= 0 ? fieldOrder.indexOf(fieldName) : -1 };
	};

	return (
		<div className="w-full">
			<div className="flex justify-end gap-4 items-center pb-2">
				<TooltipProvider>
					<Tooltip>
						<TooltipTrigger asChild>
							<Button data-clickid={'Action: Invoice Manager - Table Refresh'} onClick={handleRefresh} className={'h-[40px]'} variant="outline">
								<RefreshCcwIcon className={`stroke-[#9ca3af]`}></RefreshCcwIcon>
							</Button>
						</TooltipTrigger>
						<TooltipContent>Last Refreshed: {convertDate(lastRefresh)}</TooltipContent>
					</Tooltip>
				</TooltipProvider>
				{rangeStartDate && (
					<DateRangePicker
						clickId={'Action: Invoice Manager - Date Range Picker'}
						onCancel={dateRangeCancelHandler}
						locale="en-US"
						initialDateFrom={rangeStartDate}
						initialDateTo={rangeEndDate}
						onUpdate={dateRangeHandler}
						showCompare={false}
					></DateRangePicker>
				)}
				<div className="flex items-center gap-2">
					<ButtonGroupDropdown onExport={excelDownloadHandler} loading={excelLoading} />
				</div>
				<Button
					data-clickid={'Action: Invoice Manager - History Button'}
					onClick={() => props?.onHistory && props.onHistory()}
					variant="outline"
					className="gap-2 h-[40px]"
				>
					History
				</Button>
				<Button data-clickid={'Action: Invoice Manager - Add Invoice/Syndicate'} onClick={() => setSheetOpen(true)} variant="default" className="gap-2 h-[40px]">
					<Plus size={18} /> Add
				</Button>
			</div>
			<div
				className={`${IS_DARK.value ? 'ag-theme-balham-dark' : 'ag-theme-balham'} w-full grid-container invoice-table`}
				style={{ height: height, minHeight: 300, overflow: 'auto' }}
			>
				<AgGridReact
					ref={gridRef}
					columnDefs={columnDefs.map((col) => {
						if (col.cellRenderer === 'ActionCellRenderer') return { ...col, cellRenderer: ActionCellRenderer };
						if (col.cellRenderer === 'StatusCellRenderer') return { ...col, cellRenderer: StatusCellRenderer };
						if (col.cellRenderer === 'CurrencyCellRenderer') return { ...col, cellRenderer: CurrencyCellRenderer };
						return { ...col, cellRenderer: DefaultCellRenderer };
					})}
					onSortChanged={(e) => {
						if (e.source != 'api') {
							saveGridSettings();
						}
					}}
					onColumnVisible={(e) => {
						if (e.source != 'api') {
							saveGridSettings();
						}
					}}
					onColumnPinned={(e) => {
						if (e.source != 'api') {
							saveGridSettings();
						}
					}}
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
			{/* Warning dialog for Coverage Delete */}
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
					{/* Warning Dialog for Unsaved Changes in the form */}
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
						<Form autocomplete="off" {...form}>
							<form autoComplete="off" className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-4 sm:gap-x-6 p-2 sm:p-4 invoice-form">
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
																className: 'default-input !h-[40px] dark:placeholder-grey-100',
																style: { pointerEvents: 'auto' },
																required: getConfig('acc_id').mandatory,
															}}
															// inputProps={{
															// 	className:
															// 		'default-input flex w-full rounded-md border border-input bg-background px-4 py-2 ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground dark:placeholder:text-grey-100 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-0 disabled:cursor-not-allowed disabled:opacity-50 h-10 text-sm sm:text-base disabled:bg-blue-100 dark:disabled:bg-gray-900',
															// 	maxLength: 32,
															// 	required: getConfig('acc_id').mandatory,
															// 	style: { pointerEvents: 'auto' },
															// }}
															menuStyle={{
																zIndex: 1050,
																width: '100%',
																position: 'absolute',
															}}
															renderMenu={renderAccountMenu}
														/>
														<div className="min-h-[20px]">
															<FormMessage />
														</div>
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
													<div>
														<Popover open={open} onOpenChange={setOpen}>
															<PopoverTrigger asChild>
																<Button
																	variant="outline"
																	disabled={getConfig('invoice_date').disabled}
																	className={
																		'w-full justify-start text-left h-10 ' +
																		'flex rounded-md border border-input bg-background px-4 py-2 ring-offset-background ' +
																		'focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-0 ' +
																		(field.value ? '' : ' text-muted-foreground') +
																		(getConfig('invoice_date').disabled ? ' opacity-50 cursor-not-allowed bg-blue-100' : '')
																	}
																	// className={
																	// 	(field.value ? '' : ' text-muted-foreground') +
																	// 	(getConfig('invoice_date').disabled ? ' opacity-50 cursor-not-allowed bg-blue-100' : '')
																	// }
																>
																	<CalendarIcon className="mr-2 h-4 w-4" />
																	{field.value ? formatDateTime(field.value) : <span>Pick a date</span>}
																</Button>
															</PopoverTrigger>
															{!getConfig('invoice_date').disabled && (
																<PopoverContent className="w-auto p-0">
																	<Calendar
																		mode="single"
																		selected={field.value ? field.value : undefined}
																		onSelect={(date) => {
																			if (!date) {
																				field.onChange('');
																				return;
																			}
																			const formattedDate = format(date, 'yyyy-MM-dd');
																			field.onChange(formattedDate);
																			setOpen(false);
																		}}
																		disabled={isFutureDate}
																		initialFocus
																	/>
																</PopoverContent>
															)}
														</Popover>
														<div className="min-h-[20px]">
															<FormMessage />
														</div>
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
													<div>
														<Input
															{...field}
															type="text"
															disabled={getConfig('invoice_no').disabled || !!editingRow}
															maxLength={256}
															className=" disabled:bg-blue-100 dark:disabled:bg-gray-900"
															placeholder="Enter invoice number"
															required={getConfig('invoice_no').mandatory}
														/>
														<div className="min-h-[20px]">
															<FormMessage />
														</div>
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
													<div>
														<Select
															value={field.value}
															onValueChange={field.onChange}
															disabled={getConfig('invoice_status').disabled}
															required={getConfig('invoice_status').mandatory}
														>
															<SelectTrigger className="w-full h-10 justify-between dark:disabled:bg-gray-900 disabled:bg-blue-100  dark:bg-inherit text-sm">
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
													<div>
														<Input
															{...field}
															type="number"
															onBlur={(e) => {
																field.onBlur();
																if (e.target.value !== '') {
																	field.onChange(parseFloat(e.target.value).toFixed(2));
																}
															}}
															disabled={getConfig('commission').disabled}
															className=" dark:disabled:bg-gray-900 disabled:bg-blue-100"
															placeholder="Enter commission amount"
															required={getConfig('commission').mandatory}
														/>
														<div className="min-h-[20px]">
															<FormMessage />
														</div>
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
													<div>
														<Select value={field.value} onValueChange={field.onChange} disabled={getConfig('product').disabled || !!editingRow}>
															<SelectTrigger className="w-full disabled:cursor-not-allowed justify-between h-10 dark:bg-inherit text-sm disabled:bg-blue-100 dark:disabled:bg-gray-900">
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
															<div>
																<AsyncTypeahead
																	id="ticker"
																	labelKey={(option) => option.ticker_symbol}
																	options={tickerOptions}
																	isLoading={isTickerLoading}
																	minLength={1}
																	placeholder="Search Ticker..."
																	onSearch={handleTickerSearch}
																	onChange={handleTickerChange}
																	selected={selectedTicker}
																	disabled={getConfig('ticker').disabled}
																	// inputProps={{
																	// 	className:
																	// 		'flex w-full rounded-md border border-input bg-background px-4 py-2 ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-0 disabled:cursor-not-allowed disabled:opacity-50 h-10 text-sm sm:text-base disabled:bg-blue-100 dark:disabled:bg-gray-900',
																	// 	maxLength: 32,
																	// }}
																	inputProps={{
																		// Use this to match the Account Name style exactly
																		className:
																			'flex h-10 w-full rounded-md border border-input bg-background px-4 py-2 text-sm sm:text-base ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-0 disabled:cursor-not-allowed disabled:bg-blue-100 disabled:opacity-50',
																		maxLength: 32,
																	}}
																	menuStyle={{
																		zIndex: 1050,
																		width: '100%',
																		position: 'absolute',
																	}}
																	renderMenu={renderTickerMenu}
																/>

																<div className="min-h-[20px]">
																	<FormMessage />
																</div>
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
															<div>
																<Input
																	{...field}
																	disabled={getConfig('quantity').disabled}
																	type="number"
																	onBlur={(e) => {
																		field.onBlur();
																		if (e.target.value !== '') {
																			field.onChange(parseFloat(e.target.value).toFixed(2));
																		}
																	}}
																	min={0}
																	className=" disabled:bg-blue-100 dark:disabled:bg-gray-900"
																	placeholder="Quantity"
																/>
																<div className="min-h-[20px]">
																	<FormMessage />
																</div>
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
															<div>
																<Input
																	{...field}
																	disabled={getConfig('price').disabled}
																	type="number"
																	onBlur={(e) => {
																		field.onBlur();
																		if (e.target.value !== '') {
																			field.onChange(parseFloat(e.target.value).toFixed(2));
																		}
																	}}
																	min={0}
																	step="any"
																	className=" disabled:bg-blue-100 dark:disabled:bg-gray-900"
																	placeholder="Price"
																/>
																<div className="min-h-[20px]">
																	<FormMessage />
																</div>
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
															<div>
																<Input
																	{...field}
																	disabled={getConfig('net_amount').disabled}
																	type="number"
																	onBlur={(e) => {
																		field.onBlur();
																		if (e.target.value !== '') {
																			field.onChange(parseFloat(e.target.value).toFixed(2));
																		}
																	}}
																	min={0}
																	step="any"
																	className=" disabled:bg-blue-100 dark:disabled:bg-gray-900"
																	placeholder="Net Amount"
																/>
																<div className="min-h-[20px]">
																	<FormMessage />
																</div>
															</div>
														</FormItem>
													)}
												/>
											)}
										</div>
									</div>
								)}

								<div className="col-span-1 sm:col-span-2 lg:col-span-3 grid grid-cols-1 lg:grid-cols-2 gap-x-4 sm:gap-x-6">
									<FormField
										control={form.control}
										rules={{
											validate: () => (form.getValues().attach || form.getValues().att_id ? true : 'Invoice attachment is required'),
										}}
										name="attach"
										render={({ field }) => (
											<FormItem>
												<FormLabel className="gap-1 font-medium">
													Attach Invoice<span className="text-red-500">*</span>
												</FormLabel>
												<div>
													<div
														className="border-2 border-dashed border-input rounded-lg p-3 sm:p-4 flex flex-col items-center justify-center bg-transparent hover:bg-gray-100 dark:hover:bg-gray-900 transition h-[100px] sm:h-[115px] cursor-pointer relative shadow-none"
														onClick={() => document.getElementById('att_id')?.click()}
													>
														<input
															type="file"
															id="att_id"
															accept={`.${INVOICE_IMAGE_VARIABLE.value.fileUpload?.fileFormat}` || '.pdf'}
															className="hidden"
															onChange={async (e) => {
																const file = e.target.files?.[0];
																if (!file) return;

																// File size check (support e.g. '40kb', '40 kb', '40KB')
																const sizeStr = INVOICE_IMAGE_VARIABLE.value.fileUpload?.fileSize || '';
																let maxSize = 0;
																if (sizeStr) {
																	// Updated regex: allow optional spaces between number and unit, case-insensitive
																	const match = sizeStr.match(/(\d+)\s*(kb|mb|b)/i);
																	if (match) {
																		const num = parseInt(match[1], 10);
																		const unit = match[2].toLowerCase();
																		if (unit === 'kb') maxSize = num * 1024;
																		else if (unit === 'mb') maxSize = num * 1024 * 1024;
																		else if (unit === 'b') maxSize = num;
																	}
																}
																if (maxSize > 0 && file.size > maxSize) {
																	toast.warning(`File size exceeds maximum allowed (${sizeStr})`);
																	e.target.value = '';
																	return;
																}

																const configFormat = INVOICE_IMAGE_VARIABLE.value.fileUpload?.fileFormat || 'pdf';
																const allowedExt = configFormat.replace('.', '').trim().toLowerCase();

																const fileExt = file.name.split('.').pop()?.toLowerCase();

																if (fileExt !== allowedExt) {
																	toast.warning(`Only ${allowedExt.toUpperCase()} files are allowed.`);
																	e.target.value = '';
																	return;
																}

																const reader = new FileReader();
																reader.onloadend = () => {
																	const base64Content = reader.result.split(',')[1];

																	try {
																		const blob = base64ToBlob(base64Content, file.type);
																		if (blob.size === 0) {
																			throw new Error('File conversion failed: Empty Blob');
																		}

																		const testUrl = URL.createObjectURL(blob);
																		form.setValue('invoiceFile', file);
																		// save base64 in `attach` (do NOT overwrite numeric backend att_id)
																		form.setValue('attach', base64Content, { shouldValidate: true });
																		form.setValue('att_name', file.name);
																		form.setValue('att_size', Math.round(file.size / 1024).toString());
																		// preserve backend att_id if editingRow provides one
																		if (editingRow && editingRow.att_id) {
																			form.setValue('att_id', editingRow.att_id);
																		}

																		URL.revokeObjectURL(testUrl);
																	} catch (error) {
																		console.error('Decryption Error:', error);
																		toast.error('The file could not be processed correctly.');
																	}
																};

																reader.readAsDataURL(file);
															}}
														/>

														{/* Outer check: show if either a newly attached file or a server att_id exists */}
														{form.watch('attach') || form.watch('att_id') ? (
															<div className="flex flex-col items-center justify-center w-full space-y-1 min-w-0">
																{/* Only show this div if we actually have a name to display */}
																{(form.watch('invoiceFile')?.name || form.watch('att_name')) && (
																	<div className="flex flex-col items-center text-center">
																		<span
																			className="text-xs sm:text-sm font-semibold text-blue-600 truncate block max-w-[300px]"
																			title={form.watch('invoiceFile')?.name || form.watch('att_name')}
																		>
																			{form.watch('invoiceFile')?.name || form.watch('att_name')}
																		</span>

																		{/* Show size if available */}
																		{(form.watch('invoiceFile') || form.watch('att_size')) && (
																			<span className="text-[10px] sm:text-xs text-gray-500">
																				(
																				{form.watch('invoiceFile')
																					? (form.watch('invoiceFile').size / 1024).toFixed(0)
																					: form.watch('att_size')}{' '}
																				KB)
																			</span>
																		)}
																	</div>
																)}

																<div className="flex gap-2 w-full max-w-[200px]">
																	{/* Download button - only if it's an existing server file */}
																	{editingRow && form.watch('att_id') && !form.watch('invoiceFile') && (
																		<Button
																			type="button"
																			variant="outline"
																			size="sm"
																			className="h-7 flex-1 text-[10px]"
																			onClick={(e) => {
																				e.stopPropagation();
																				downloadInvoice(editingRow);
																			}}
																		>
																			<Download className="mr-1" size={12} /> Download
																		</Button>
																	)}

																	<Button
																		type="button"
																		variant="destructive"
																		size="sm"
																		className="h-7 flex-1 text-[10px]"
																		onClick={(e) => {
																			e.stopPropagation();
																			// Clear only newly attached data; keep backend att_id
																			form.setValue('attach', '', { shouldValidate: true });
																			form.setValue('invoiceFile', null);
																			form.setValue('att_name', '');
																			form.setValue('att_size', '');
																			form.setValue('att_url', '');
																			form.setValue('att_id', '');
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
													<div>
														<Textarea
															{...field}
															id="notes"
															rows={4}
															disabled={getConfig('comments').disabled}
															className="shadow-none max-h-[150px] disabled:bg-blue-100 min-h-[115px] dark:disabled:bg-gray-900"
															placeholder="Enter notes here..."
															required={getConfig('comments').mandatory}
														></Textarea>

														{/* <textarea
														{...field}
														id="notes"
														rows={4}
														disabled={getConfig('comments').disabled}
														className="border rounded w-full min-w-0 p-2 text-sm sm:text-base focus:outline-none focus:ring-1 focus:ring-primary disabled:opacity-50 disabled:bg-gray-100"
														placeholder="Enter notes here..."
														required={getConfig('comments').mandatory}
													/> */}
														<div className="min-h-[20px]">
															<FormMessage />
														</div>
													</div>
												</FormItem>
											)}
										/>
									)}
								</div>
							</form>
						</Form>

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
												className:
													'h-10 w-full border rounded-md px-3 py-2 text-sm sm:text-base focus:outline-none focus:outline-none focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-0 focus:ring-1 focus:ring-primary',
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
												value={percentage === undefined || percentage === null ? '' : percentage}
												onChange={(e) => {
													let val = e.target.value;
													// Allow empty string for erasing
													if (val !== '' && parseFloat(val) > 100) {
														val = '100';
													}
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
												onBlur={(e) => {
													let val = parseFloat(e.target.value);
													if (!isNaN(val)) {
														if (val > 100) val = 100;
														// Format to 2 decimals on blur
														const formattedVal = val.toFixed(2);
														setPercentage(formattedVal);
													}
												}}
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
											<tbody className={IS_DARK.value ? 'bg-[#181d1f] text-gray-300' : 'bg-white text-gray-700'}>
												{coverageRows.map((row, index) => (
													<tr key={row.id || index} style={{ height: 34 }} className="border-b hover:bg-gray-50/5 transition-colors">
														<td className="px-4 truncate">{row.display_name}</td>
														<td className="px-4 truncate">{row.corp_title}</td>
														<td className="px-4 truncate">{row.email}</td>
														<td className="px-4">
															<Input
																type="number"
																value={row.percentage === undefined || row.percentage === null ? '' : row.percentage}
																onChange={(e) => {
																	let val = e.target.value;
																	// Allow empty string for erasing
																	if (val !== '' && parseFloat(val) > 100) {
																		val = '100';
																	}
																	const updatedRows = coverageRows.map((r) => (r.email === row.email ? { ...r, percentage: val } : r));
																	setCoverageRows(updatedRows);
																}}
																onBlur={(e) => {
																	let val = parseFloat(e.target.value);
																	if (!isNaN(val)) {
																		if (val > 100) val = 100;
																		// Format to 2 decimals on blur
																		const formattedVal = val.toFixed(2);
																		const updatedRows = coverageRows.map((r) =>
																			r.email === row.email ? { ...r, percentage: formattedVal } : r
																		);
																		setCoverageRows(updatedRows);
																	} else {
																		// If invalid, clear
																		const updatedRows = coverageRows.map((r) => (r.email === row.email ? { ...r, percentage: '' } : r));
																		setCoverageRows(updatedRows);
																	}
																}}
																className="w-full h-8 rounded-md px-2 transition-all"
																min="0"
																max="100"
																step="0.01" // Allows decimal increments
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
																{/* Coverage Delete warning dialog */}
																<Dialog open={showCoverageDeleteDialog} onOpenChange={setShowCoverageDeleteDialog}>
																	<DialogContent>
																		<DialogHeader>
																			<DialogTitle>Delete Coverage</DialogTitle>
																		</DialogHeader>
																		<div>Are you sure you want to delete this coverage entry? This action cannot be undone.</div>
																		<DialogFooter className="flex justify-end gap-2">
																			<Button variant="outline" onClick={handleCoverageDeleteCancel}>
																				Cancel
																			</Button>
																			<Button variant="destructive" onClick={handleCoverageDeleteConfirm}>
																				Delete
																			</Button>
																		</DialogFooter>
																	</DialogContent>
																</Dialog>
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
			{(getInvoiceDetailsMutation.isLoading || saveInvoiceMutation.isLoading || updateInvoiceMutation.isLoading || columnDefs.length === 0) && (
				<OverlayLoader></OverlayLoader>
			)}
		</div>
	);
}

--------------------------------------------------------------------------------------------------------
	{
  "fileUpload": {
    "fileFormat": "pdf",
    "fileSize": "10MB"
  },
  "formConfig": {
    "extraFormFields": [
      {
        "name": "dyn_code",
        "label": "Code",
        "placeholder": "Enter code",
        "type": "string",
        "mandatory": true,
        "pattern": {},
        "patternMessage": "Only uppercase letters & numbers allowed",
        "readOnly": false,
        "disabled": false,
        "defaultValue": "",
        "className": ""
      },
      {
        "name": "dyn_description",
        "label": "Description",
        "placeholder": "Enter description",
        "type": "string",
        "mandatory": false,
        "readOnly": false,
        "disabled": false,
        "defaultValue": "",
        "className": ""
      },
      {
        "name": "dyn_quantity",
        "label": "Quantity",
        "placeholder": "Enter Quantity",
        "type": "number",
        "mandatory": false,
        "pattern": {},
        "patternMessage": "Only numbers allowed",
        "readOnly": false,
        "disabled": false,
        "defaultValue": 0,
        "className": ""
      },
      {
        "name": "dyn_amount",
        "label": "Amount",
        "placeholder": "Enter Amount",
        "type": "number",
        "mandatory": false,
        "readOnly": true,
        "disabled": true,
        "defaultValue": 100,
        "className": ""
      }
    ],
    "formFields": [
      {
        "name": "acc_id",
        "mandatory": true,
        "disabled": false,
        "hidden": false
      },
      {
        "name": "invoice_no",
        "mandatory": true,
        "disabled": false,
        "hidden": false
      },
      {
        "name": "invoice_date",
        "mandatory": false,
        "disabled": false,
        "hidden": false
      },
      {
        "name": "invoice_status",
        "mandatory": true,
        "disabled": false,
        "hidden": false,
        "options": [
          {
            "label": "Open",
            "value": "open"
          },
          {
            "label": "Paid",
            "value": "paid"
          }
        ]
      },
      {
        "name": "commission",
        "mandatory": true,
        "disabled": false,
        "hidden": false
      },
      {
        "name": "product",
        "mandatory": true,
        "disabled": false,
        "hidden": false,
        "options": [
          {
            "label": "Syndicate",
            "value": "syndicate"
          },
          {
            "label": "Invoice",
            "value": "invoice"
          }
        ]
      },
      {
        "name": "ticker",
        "mandatory": true,
        "disabled": false,
        "hidden": false
      },
      {
        "name": "price",
        "mandatory": true,
        "disabled": false,
        "hidden": false
      },
      {
        "name": "quantity",
        "mandatory": true,
        "disabled": false,
        "hidden": false
      },
      {
        "name": "payOut",
        "mandatory": false,
        "disabled": false,
        "hidden": false
      },
      {
        "name": "net_amount",
        "mandatory": true,
        "disabled": false,
        "hidden": false
      },
      {
        "name": "comments",
        "mandatory": false,
        "disabled": false,
        "hidden": false
      }
    ]
  },
  "defaultDateRange": {
    "dateFilterDefaultRange": "7"
  }
}

