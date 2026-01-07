const STATIC_INVOICE_CONFIG = {
  fileUpload: {
    fileFormat: 'pdf',
    fileSize: '10MB',
  },
  formConfig: {
    extraFormFields: [
      {
        name: 'dyn_code',
        label: 'Code',
        placeholder: 'Enter code',
        type: 'string',
        mandatory: true,
        pattern: {},
        patternMessage: 'Only uppercase letters & numbers allowed',
        readOnly: false,
        disabled: false,
        defaultValue: '',
        className: '',
      },
      {
        name: 'dyn_description',
        label: 'Description',
        placeholder: 'Enter description',
        type: 'string',
        mandatory: false,
        readOnly: false,
        disabled: false,
        defaultValue: '',
        className: '',
      },
      {
        name: 'dyn_quantity',
        label: 'Quantity',
        placeholder: 'Enter Quantity',
        type: 'number',
        mandatory: false,
        pattern: {},
        patternMessage: 'Only numbers allowed',
        readOnly: false,
        disabled: false,
        defaultValue: 0,
        className: '',
      },
      {
        name: 'dyn_amount',
        label: 'Amount',
        placeholder: 'Enter Amount',
        type: 'number',
        mandatory: false,
        readOnly: true,
        disabled: true,
        defaultValue: 100,
        className: '',
      },
    ],

	formFields: [
  {
    "name": "acc_id",
    "label": "Account Name",
    "placeholder": "Search Account Name",
    "type": "typeahead",  // or "async-typeahead"
    "mandatory": true,
    "disabled": false,
    "hidden": false,
    "readOnly": false,
    "pattern": null,
    "patternMessage": "",
    "defaultValue": "",
    "className": "",
    "options": []
  },
  {
    "name": "invoice_date",
    "label": "Invoice Date",
    "placeholder": "Pick a date",
    "type": "date",
    "mandatory": false,
    "disabled": false,
    "hidden": false,
    "readOnly": false,
    "pattern": null,
    "patternMessage": "",
    "defaultValue": "",
    "className": ""
  },
  {
    "name": "invoice_no",
    "label": "Invoice Number",
    "placeholder": "Enter invoice number",
    "type": "text",
    "mandatory": true,
    "disabled": false,
    "hidden": false,
    "readOnly": false,
    "pattern": null,
    "patternMessage": "",
    "defaultValue": "",
    "className": "",
    "maxLength": 256
  },
  {
    "name": "invoice_status",
    "label": "Invoice Status",
    "placeholder": "Select status",
    "type": "select",
    "mandatory": true,
    "disabled": false,
    "hidden": false,
    "readOnly": false,
    "pattern": null,
    "patternMessage": "",
    "defaultValue": "",
    "className": "",
    "options": [
      { "label": "Open", "value": "open" },
      { "label": "Paid", "value": "paid" }
    ]
  },
  {
    "name": "commission",
    "label": "Commission",
    "placeholder": "Enter commission amount",
    "type": "number",
    "mandatory": true,
    "disabled": false,
    "hidden": false,
    "readOnly": false,
    "pattern": null,
    "patternMessage": "",
    "defaultValue": "",
    "className": "",
    "min": 0,
    "step": "0.01"
  },
  {
    "name": "product",
    "label": "Product",
    "placeholder": "Select product",
    "type": "select",
    "mandatory": true,
    "disabled": false,
    "hidden": false,
    "readOnly": false,
    "pattern": null,
    "patternMessage": "",
    "defaultValue": "",
    "className": "",
    "options": [
      { "label": "Syndicate", "value": "syndicate" },
      { "label": "Invoice", "value": "invoice" }
    ]
  },
  {
    "name": "ticker",
    "label": "Ticker",
    "placeholder": "Search Ticker...",
    "type": "async-typeahead",
    "mandatory": true,
    "disabled": false,
    "hidden": false,
    "readOnly": false,
    "pattern": null,
    "patternMessage": "",
    "defaultValue": "",
    "className": "",
    "syndicateOnly": true
  },
  {
    "name": "quantity",
    "label": "Quantity",
    "placeholder": "Quantity",
    "type": "number",
    "mandatory": true,
    "disabled": false,
    "hidden": false,
    "readOnly": false,
    "pattern": null,
    "patternMessage": "",
    "defaultValue": "",
    "className": "",
    "min": 0,
    "step": "0.01",
    "syndicateOnly": true
  },
  {
    "name": "price",
    "label": "Price",
    "placeholder": "Price",
    "type": "number",
    "mandatory": true,
    "disabled": false,
    "hidden": false,
    "readOnly": false,
    "pattern": null,
    "patternMessage": "",
    "defaultValue": "",
    "className": "",
    "min": 0,
    "step": "any",
    "syndicateOnly": true
  },
  {
    "name": "net_amount",
    "label": "Net Amount",
    "placeholder": "Net Amount",
    "type": "number",
    "mandatory": true,
    "disabled": false,
    "hidden": false,
    "readOnly": false,
    "pattern": null,
    "patternMessage": "",
    "defaultValue": "",
    "className": "",
    "min": 0,
    "step": "any",
    "syndicateOnly": true
  },
  {
    "name": "comments",
    "label": "Notes",
    "placeholder": "Enter notes here...",
    "type": "textarea",
    "mandatory": false,
    "disabled": false,
    "hidden": false,
    "readOnly": false,
    "pattern": null,
    "patternMessage": "",
    "defaultValue": "",
    "className": "",
    "rows": 4
  }
]
  },
  defaultDateRange: {
    dateFilterDefaultRange: '7',
  },
};




const fields = STATIC_INVOICE_CONFIG.formConfig?.formFields || [];
// console.log('Dynamic Fields Config:', fields);

const fieldConfigs = fields.reduce((acc, field, idx) => {
    acc[field.name] = {
		label: field.label || '',
		placeholder: field.placeholder || '',
		type: field.type ,
		readOnly: field.readOnly ?? false,
		pattern: field.pattern || null,
		patternMessage: field.patternMessage || '',
		min: field.min,
		max: field.max,
		step: field.step,
		maxLength: field.maxLength,
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

const renderField = (fieldName) => {
    const config = getConfig(fieldName);
    // console.log(config);
    if (config.hidden) return null;

    // Build validation rules
    const rules = {};
    if (config.mandatory && !config.readOnly && !config.disabled) {
        rules.required = `${config.label} is required`;
    }
    if (config.pattern) {
        rules.pattern = {
            value: new RegExp(config.pattern),
            message: config.patternMessage || 'Invalid format',
        };
    }
    if (config.min !== undefined) {
        rules.min = {
            value: config.min,
            message: `${config.label} cannot be less than ${config.min}`,
        };
    }

    // Render based on type
    switch (config.type) {
        case 'typeahead':
            if (fieldName === 'acc_id') {
                return (
                    <FormField
                        key={fieldName}
                        control={form.control}
                        name={fieldName}
                        rules={rules}
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel className={`gap-1 font-medium ${config.className}`}>
                                    {config.label}
                                    {config.mandatory && <span className="text-red-500">*</span>}
                                </FormLabel>
                                <div style={{ position: 'relative' }}>
                                    <AsyncTypeahead
                                        id={fieldName}
                                        labelKey="account_name"
                                        options={accountOptions}
                                        isLoading={isLoading}
                                        minLength={1}
                                        placeholder={config.placeholder}
                                        onSearch={handleAccountSearch}
                                        onChange={handleAccountChange}
                                        selected={selectedAccount}
                                        disabled={config.disabled || !!editingRow}
                                        inputProps={{
                                            className: `default-input !h-[40px] dark:placeholder-grey-100 ${config.className}`,
                                            style: { pointerEvents: 'auto' },
                                            required: config.mandatory,
                                        }}
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
                );
            } else if (fieldName === 'ticker') {
                return (
                    <FormField
                        key={fieldName}
                        control={form.control}
                        name={fieldName}
                        rules={rules}
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel className={`gap-1 font-medium text-sm ${config.className}`}>
                                    {config.label}
                                    {config.mandatory && <span className="text-red-500">*</span>}
                                </FormLabel>
                                <div>
                                    <AsyncTypeahead
                                        id={fieldName}
                                        labelKey={(option) => option.ticker_symbol}
                                        options={tickerOptions}
                                        isLoading={isTickerLoading}
                                        minLength={1}
                                        placeholder={config.placeholder}
                                        onSearch={handleTickerSearch}
                                        onChange={handleTickerChange}
                                        selected={selectedTicker}
                                        disabled={config.disabled}
                                        inputProps={{
                                            className: `flex h-10 w-full rounded-md border border-input bg-background px-4 py-2 text-sm sm:text-base ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-0 disabled:cursor-not-allowed disabled:bg-blue-100 disabled:opacity-50 ${config.className}`,
                                            maxLength: config.maxLength || 32,
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
                );
            }
            break;

        case 'date':
            return (
                <FormField
                    key={fieldName}
                    control={form.control}
                    name={fieldName}
                    rules={rules}
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel className={`gap-1 font-medium ${config.disabled ? 'opacity-50' : ''} ${config.className}`}>
                                {config.label}
                                {config.mandatory && <span className="text-red-500">*</span>}
                            </FormLabel>
                            <div>
                                <Popover open={open} onOpenChange={setOpen}>
                                    <PopoverTrigger asChild>
                                        <Button
                                            variant="outline"
                                            disabled={config.disabled}
                                            className={
                                                'w-full justify-start text-left h-10 ' +
                                                'flex rounded-md border border-input bg-background px-4 py-2 ring-offset-background ' +
                                                'focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-0 ' +
                                                (field.value ? '' : ' text-muted-foreground') +
                                                (config.disabled ? ' opacity-50 cursor-not-allowed bg-blue-100' : '') +
                                                ` ${config.className}`
                                            }
                                        >
                                            <CalendarIcon className="mr-2 h-4 w-4" />
                                            {field.value ? formatDateTime(field.value) : <span>{config.placeholder || 'Pick a date'}</span>}
                                        </Button>
                                    </PopoverTrigger>
                                    {!config.disabled && (
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
            );

        case 'select':
            return (
                <FormField
                    key={fieldName}
                    control={form.control}
                    name={fieldName}
                    rules={rules}
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel className={`gap-1 font-medium ${config.syndicateOnly ? 'text-sm' : ''} ${config.className}`}>
                                {config.label}
                                {config.mandatory && <span className="text-red-500">*</span>}
                            </FormLabel>
                            <div>
                                <Select
                                    value={field.value}
                                    onValueChange={field.onChange}
                                    disabled={config.disabled || (fieldName === 'product' && !!editingRow)}
                                    required={config.mandatory}
                                >
                                    <SelectTrigger className={`w-full h-10 justify-between dark:disabled:bg-gray-900 disabled:bg-blue-100 dark:bg-inherit text-sm disabled:cursor-not-allowed ${config.className}`}>
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

        case 'number':
            return (
                <FormField
                    key={fieldName}
                    control={form.control}
                    name={fieldName}
                    rules={rules}
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel className={`gap-1 font-medium ${config.syndicateOnly ? 'text-sm' : ''} ${config.className}`}>
                                {config.label}
                                {config.mandatory && <span className="text-red-500">*</span>}
                            </FormLabel>
                            <div>
                                <Input
                                    {...field}
                                    type="number"
                                    disabled={config.disabled || config.readOnly}
                                    onBlur={(e) => {
                                        field.onBlur();
                                        if (e.target.value !== '' && config.step) {
                                            const decimals = config.step === 'any' ? 2 : (config.step.toString().split('.')[1]?.length || 0);
                                            field.onChange(parseFloat(e.target.value).toFixed(decimals));
                                        }
                                    }}
                                    min={config.min}
                                    max={config.max}
                                    step={config.step}
                                    className={`dark:disabled:bg-gray-900 disabled:bg-blue-100 ${config.className}`}
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

        case 'textarea':
            return (
                <FormField
                    key={fieldName}
                    control={form.control}
                    name={fieldName}
                    rules={rules}
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel className={`gap-1 font-medium ${config.className}`}>
                                {config.label}
                                {config.mandatory && <span className="text-red-500">*</span>}
                            </FormLabel>
                            <div>
                                <Textarea
                                    {...field}
                                    id={fieldName}
                                    rows={config.rows || 4}
                                    disabled={config.disabled || config.readOnly}
                                    className={`shadow-none max-h-[150px] disabled:bg-blue-100 min-h-[115px] dark:disabled:bg-gray-900 ${config.className}`}
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

        case 'text':
        default:
            return (
                <FormField
                    key={fieldName}
                    control={form.control}
                    name={fieldName}
                    rules={rules}
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel className={`gap-1 font-medium ${config.className}`}>
                                {config.label}
                                {config.mandatory && <span className="text-red-500">*</span>}
                            </FormLabel>
                            <div>
                                <Input
                                    {...field}
                                    type="text"
                                    disabled={config.disabled || config.readOnly || (fieldName === 'invoice_no' && !!editingRow)}
                                    maxLength={config.maxLength}
                                    className={`disabled:bg-blue-100 dark:disabled:bg-gray-900 ${config.className}`}
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

// Separate syndicate fields from main fields
const syndicateFields = ['ticker', 'quantity', 'price', 'net_amount'];
const mainFields = fieldOrder.filter(f => !syndicateFields.includes(f));


<form autoComplete="off" className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-4 sm:gap-x-6 p-2 sm:p-4 invoice-form">
								{/* Render main fields dynamically based on order */}
								{mainFields.map((fieldName) => {
									return <div key={fieldName}>{renderField(fieldName)}</div>;
								})}

								{/* Syndicate section - conditionally rendered */}
								{watchedStatus === 'syndicate' && syndicateFields.length > 0 && (
									<div className="col-span-1 sm:col-span-2 lg:col-span-3 grid grid-cols-1 gap-0 mt-2 sm:mt-6">
										<div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-2 sm:p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4 mb-4">
											{syndicateFields.map((fieldName) => renderField(fieldName))}
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
