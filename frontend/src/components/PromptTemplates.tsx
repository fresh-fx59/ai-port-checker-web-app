interface PromptTemplate {
  title: string;
  description: string;
  template: string;
}

interface PromptTemplatesProps {
  onSelectTemplate: (template: string) => void;
  isVisible: boolean;
  onToggle: () => void;
}

const templates: PromptTemplate[] = [
  {
    title: "Business Analysis",
    description: "Analyze business data and provide insights",
    template: "Please analyze the following business scenario and provide detailed insights:\n\n[Describe your business situation here]\n\nPlease include:\n- Key findings\n- Recommendations\n- Potential risks\n- Next steps"
  },
  {
    title: "Technical Documentation",
    description: "Generate technical documentation",
    template: "Create comprehensive technical documentation for:\n\n[Describe your system/feature here]\n\nPlease include:\n- Overview and purpose\n- Technical specifications\n- Implementation details\n- Usage examples\n- Best practices"
  },
  {
    title: "Market Research",
    description: "Conduct market research analysis",
    template: "Conduct a market research analysis for:\n\n[Describe your product/service/market here]\n\nPlease analyze:\n- Market size and trends\n- Target audience\n- Competitive landscape\n- Opportunities and challenges\n- Strategic recommendations"
  },
  {
    title: "Content Strategy",
    description: "Develop content marketing strategy",
    template: "Develop a content strategy for:\n\n[Describe your brand/business here]\n\nPlease include:\n- Content themes and topics\n- Target audience personas\n- Content calendar suggestions\n- Distribution channels\n- Success metrics"
  }
];

function PromptTemplates({ onSelectTemplate, isVisible, onToggle }: PromptTemplatesProps) {
  if (!isVisible) {
    return (
      <button
        onClick={onToggle}
        className="text-sm text-blue-600 hover:text-blue-800 underline mb-4"
      >
        Use a template to get started
      </button>
    );
  }

  return (
    <div className="mb-4">
      <div className="flex justify-between items-center mb-3">
        <h3 className="text-sm font-medium text-gray-700">Choose a template:</h3>
        <button
          onClick={onToggle}
          className="text-sm text-gray-500 hover:text-gray-700"
        >
          Hide templates
        </button>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {templates.map((template, index) => (
          <div
            key={index}
            className="border border-gray-200 rounded-md p-3 hover:border-blue-300 hover:bg-blue-50 cursor-pointer transition-colors"
            onClick={() => {
              onSelectTemplate(template.template);
              onToggle();
            }}
          >
            <h4 className="font-medium text-gray-900 text-sm mb-1">
              {template.title}
            </h4>
            <p className="text-xs text-gray-600">
              {template.description}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}

export default PromptTemplates;